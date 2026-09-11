package com.nnp.dashboard.service;

import com.nnp.dashboard.model.*;
import com.nnp.dashboard.repo.*;
import com.nnp.dashboard.utils.Utils;
import com.nnp.dashboard.vo.mail.EmailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private final NnpAccountRepo accountRepo;
    private final NnpAccBillRepo billRepo;
    private final NnpAccBillLnRepo billLnRepo;
    private final NnpAccCommunicationRepo commRepo;
    private final EnvironmentRepoV3 envRepo;
    private final UserRepoV2 userRepo;
    private final TokenUsageRepo tokenUsageRepo;
    private final K8sComponentStatusService k8sComponentStatusService;
    private final MailService mailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    @Value("${mail.user.welcome.from.email:no-reply@nnp.example.com}")
    private String senderEmail;
    /**
     * Generates daily bill line and updates header bill for an account on a given date.
     */
    @Transactional
    public void generateDailyBillForAccount(NnpAccount account, LocalDate billingDate) {
        String accId = account.getAccId();
        LocalDateTime startOfDay = billingDate.atStartOfDay();
        LocalDateTime endOfDay = billingDate.atTime(23, 59, 59);

        // Idempotency check: skip if daily line already generated
        if (billLnRepo.existsByAccBill_NnpAccount_AccIdAndAccBillLnDtBetween(accId, startOfDay, endOfDay)) {
            log.info("Daily bill line already exists for account {} on {}, skipping", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(billingDate));
            return;
        }

        // 1. Compute Token Usage Cost
        ZonedDateTime zStart = startOfDay.atZone(ZoneId.systemDefault());
        ZonedDateTime zEnd = endOfDay.atZone(ZoneId.systemDefault());
        List<TokenUsage> tokenUsages = tokenUsageRepo.findByAccIdAndExecdttimeBetween(accId, zStart, zEnd);

        BigDecimal tokenDayCost = BigDecimal.ZERO;
        for (TokenUsage tu : tokenUsages) {
            if (tu.getInputCost() != null) tokenDayCost = tokenDayCost.add(tu.getInputCost());
            if (tu.getOutputCost() != null) tokenDayCost = tokenDayCost.add(tu.getOutputCost());
        }
        tokenDayCost = tokenDayCost.setScale(4, RoundingMode.HALF_UP);

        // 2. Fetch K8s Running Pods for Component Active Check
        Set<String> runningPods = Collections.emptySet();
        if (account.getEnv() != null) {
            Optional<EnvironmentV3> envOpt = envRepo.findById(account.getEnv());
            if (envOpt.isPresent()) {
                EnvironmentV3 env = envOpt.get();
                String namespace = env.getEnvNamespace() != null ? env.getEnvNamespace() : env.getEnvCode();
                String token = env.getAdminK8sNsToken();
                runningPods = k8sComponentStatusService.getRunningPodNames(namespace, token);
            }
        }

        // 3. Compute Active Component Cost
        BigDecimal compDayCost = BigDecimal.ZERO;
        int activeCompCount = 0;

        Optional<NnpAccountPlan> activePlanOpt = account.getAccountPlans().stream()
                .filter(ap -> Boolean.TRUE.equals(ap.getActive()))
                .findFirst();

        String firstAccPlanCompId = null;

        if (activePlanOpt.isPresent()) {
            NnpAccountPlan activePlan = activePlanOpt.get();
            if (activePlan.getAccountPlanComps() != null) {
                for (NnpAccountPlanComp apc : activePlan.getAccountPlanComps()) {
                    if (!Boolean.TRUE.equals(apc.getActive())) continue;

                    if (firstAccPlanCompId == null) {
                        firstAccPlanCompId = apc.getAccPlanCompId();
                    }

                    HostPlanComp planComp = apc.getNnpPlanComp();
                    if (planComp == null) continue;

                    BBComponent bbComp = planComp.getEnvBBComp();
                    String k8sCompName = bbComp != null ? bbComp.getK8sCompName() : null;

                    boolean isActivePod = false;
                    if (!runningPods.isEmpty() && k8sCompName != null && !k8sCompName.isBlank()) {
                        final String compPrefix = k8sCompName.trim().toLowerCase();
                        isActivePod = runningPods.stream().anyMatch(pod -> pod.toLowerCase().startsWith(compPrefix));
                    } else {
                        // Fallback to active flag set in NnpAccountPlanComp (e.g. from frontend check-pods or DB)
                        isActivePod = Boolean.TRUE.equals(apc.getActive());
                    }

                    if (isActivePod) {
                        BigDecimal basePrice = parseBigDecimal(planComp.getHostBaseMNPr());
                        compDayCost = compDayCost.add(basePrice);
                        activeCompCount++;
                    } else {
                        log.info("Component {} (k8s: {}) is DOWN or not running for account {}, skipping charge",
                                Utils.sanitizeForLog(bbComp != null ? bbComp.getCompName() : "unknown"), Utils.sanitizeForLog(k8sCompName), Utils.sanitizeForLog(accId));
                    }
                }
            }
        }
        compDayCost = compDayCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalDayCost = tokenDayCost.add(compDayCost).setScale(4, RoundingMode.HALF_UP);

        if (totalDayCost.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Total daily cost for account {} on {} is 0.0000, skipping bill line creation", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(billingDate));
            return;
        }

        // 4. Find or Create Open Header Bill
        NnpAccBill openBill = billRepo.findFirstByNnpAccount_AccIdAndAccBillStatusOrderByAccBillDtDesc(accId, "OPEN")
                .orElseGet(() -> {
                    NnpAccBill newBill = new NnpAccBill();
                    newBill.setAccBillId(nextId("ACC_BILL"));
                    newBill.setNnpAccount(account);
                    newBill.setAccBillStatus("OPEN");
                    newBill.setAccBillDt(ZonedDateTime.now(ZoneId.systemDefault()));
                    newBill.setAccBillAmount(0.0f);
                    newBill.setBillPeriodStart(billingDate);
                    return billRepo.save(newBill);
                });

        // 5. Create Daily Bill Line Item
        NnpAccBillLn billLn = new NnpAccBillLn();
        billLn.setAccBillLnId(nextId("ACC_BILL_LN"));
        billLn.setAccBill(openBill);
        billLn.setAccBillLnDt(startOfDay);
        billLn.setTokenCost(tokenDayCost);
        billLn.setCompCost(compDayCost);
        billLn.setAccCompCharge(firstAccPlanCompId);
        billLn.setActiveCompCount(activeCompCount);
        billLn.setAccBillLnAmount(totalDayCost.floatValue());
        billLnRepo.save(billLn);

        // 6. Update Header Bill Total Amount
        float currentAmount = openBill.getAccBillAmount() != null ? openBill.getAccBillAmount() : 0.0f;
        openBill.setAccBillAmount(currentAmount + totalDayCost.floatValue());
        if (openBill.getBillPeriodEnd() == null || billingDate.isAfter(openBill.getBillPeriodEnd())) {
            openBill.setBillPeriodEnd(billingDate);
        }
        billRepo.save(openBill);

        log.info("Successfully generated daily bill line for account {}: tokenCost={}, compCost={}, total={}",
                Utils.sanitizeForLog(accId), Utils.sanitizeForLog(tokenDayCost), Utils.sanitizeForLog(compDayCost), Utils.sanitizeForLog(totalDayCost));
    }

    /**
     * Closes the current billing cycle for an account using yesterday as the closing date.
     *
     * Called by the nightly {@link com.nnp.dashboard.scheduler.BillingCycleScheduler}.
     * Delegates entirely to {@link #closeBillingCycleFromDate(NnpAccount, LocalDate)} so
     * the scheduler and the manual admin endpoint follow exactly the same rules:
     *  - ACC_BILL_NNNNNN sequence-based IDs
     *  - OPEN → PENDING status transition
     *  - billPeriodEnd set to yesterday
     *  - Next cycle opens from today with a new ACC_BILL_NNNNNN header
     *  - Invoice email dispatched using bill's own period dates
     *  - ACC_COMM_NNNNNN communication record written
     */
    @Transactional
    public void closeBillingCycle(NnpAccount account) {
        LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        log.info("Scheduler closing billing cycle for account {} at {}", Utils.sanitizeForLog(account.getAccId()), Utils.sanitizeForLog(yesterday));
        closeBillingCycleFromDate(account, yesterday);
    }

    /**
     * Manually triggers total billing calculation and sends email to account for custom date range.
     */
    @Transactional
    public void sendBillManually(String accId, LocalDate fromDate, LocalDate toDate) {
        NnpAccount account = accountRepo.findById(accId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accId));

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(23, 59, 59);

        List<NnpAccBillLn> lines = billLnRepo.findByAccBill_NnpAccount_AccIdAndAccBillLnDtBetween(accId, start, end);

        BigDecimal tokenTotal = BigDecimal.ZERO;
        BigDecimal compTotal = BigDecimal.ZERO;
        for (NnpAccBillLn ln : lines) {
            if (ln.getTokenCost() != null) tokenTotal = tokenTotal.add(ln.getTokenCost());
            if (ln.getCompCost() != null) compTotal = compTotal.add(ln.getCompCost());
        }

        tokenTotal = tokenTotal.setScale(4, RoundingMode.HALF_UP);
        compTotal = compTotal.setScale(4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = tokenTotal.add(compTotal).setScale(4, RoundingMode.HALF_UP);

        Optional<EnvironmentV3> envOpt = (account.getEnv() != null)
                ? envRepo.findById(account.getEnv())
                : Optional.empty();

        List<UserV2> users = userRepo.findByAccId(accId);
        Optional<UserV2> userOpt = users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));

        String userEmail = envOpt.map(EnvironmentV3::getEnvEmail)
                .filter(e -> !e.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getEmailId).orElse(null));

        String userName = envOpt.map(EnvironmentV3::getEnvCustName)
                .filter(n -> !n.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getFirstName).orElse(account.getAccName()));

        Map<String, String> templateData = new HashMap<>();
        templateData.put("accountName", account.getAccName());
        templateData.put("custName",    envOpt.map(EnvironmentV3::getEnvCustName).orElse(""));
        templateData.put("custId",      envOpt.map(EnvironmentV3::getEnvCustId).orElse(""));
        templateData.put("envCode",     envOpt.map(EnvironmentV3::getEnvCode).orElse(""));
        templateData.put("periodStart",  fromDate.format(DATE_FORMATTER));
        templateData.put("periodEnd",    toDate.format(DATE_FORMATTER));
        templateData.put("tokenCost",    tokenTotal.toString());
        templateData.put("compCost",     compTotal.toString());
        templateData.put("totalAmount",  grandTotal.toString());

        String subject = "NNP Platform Manual Statement (" + fromDate.format(DATE_FORMATTER) + " to " + toDate.format(DATE_FORMATTER) + ")";
        if (userEmail != null && !userEmail.isBlank()) {
            EmailVO emailVO = new EmailVO();
            emailVO.setFrom(senderEmail);
            emailVO.setFromName("NNP Platform Billing");
            emailVO.setTo(userEmail);
            emailVO.setToName(userName);
            emailVO.setSubject(subject);
            emailVO.setTemplateName("billing_invoice.ftl");
            emailVO.setTemplateDataMap(templateData);
            try {
                mailService.sendUserWelcomeMail(emailVO);
            } catch (Exception e) {
                log.error("Failed to send manual billing email for account {}: {}", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(e.getMessage()));
            }
        }

        NnpAccComm comm = new NnpAccComm();
        comm.setAccCommId(nextId("ACC_COMM"));
        comm.setNnpAccount(account);
        comm.setCommType("BILLING_MANUAL");
        comm.setCommCategory("INVOICE_MANUAL");
        comm.setCommTitle(subject);
        comm.setCommDescription(String.format("Manual statement sent. Period: %s to %s, Total: $%s",
                fromDate, toDate, grandTotal));
        comm.setCommDate(ZonedDateTime.now());
        comm.setCreatedOn(ZonedDateTime.now());
        comm.setCreatedBy("ADMIN");
        commRepo.save(comm);

        log.info("Sent manual billing statement for account {} from {} to {}. Total: ${}", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(fromDate), Utils.sanitizeForLog(toDate), Utils.sanitizeForLog(grandTotal));
    }

    /**
     * Sends an invoice email for a specific bill header using its own
     * billPeriodStart and billPeriodEnd dates.
     *
     * Unlike sendBillManually() which queries bill lines by a custom date range,
     * this method uses the bill lines already linked to the given bill header —
     * so the period shown in the email exactly matches the bill's recorded period.
     *
     * @param billId the acc_bill_id to send invoice for
     */
    @Transactional
    public NnpAccBill sendInvoiceForBill(String billId) {
        NnpAccBill bill = billRepo.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        NnpAccount account = bill.getNnpAccount();
        if (account == null) {
            throw new IllegalStateException("Bill " + billId + " has no associated account");
        }
        String accId = account.getAccId();

        // Sum totals from lines directly linked to this bill header
        List<NnpAccBillLn> lines = billLnRepo.findByAccBill_AccBillId(billId);
        BigDecimal tokenTotal = BigDecimal.ZERO;
        BigDecimal compTotal  = BigDecimal.ZERO;
        for (NnpAccBillLn ln : lines) {
            if (ln.getTokenCost() != null) tokenTotal = tokenTotal.add(ln.getTokenCost());
            if (ln.getCompCost()  != null) compTotal  = compTotal.add(ln.getCompCost());
        }
        tokenTotal = tokenTotal.setScale(4, RoundingMode.HALF_UP);
        compTotal  = compTotal.setScale(4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = tokenTotal.add(compTotal).setScale(4, RoundingMode.HALF_UP);

        // Use the bill's own recorded period dates
        String periodStart = bill.getBillPeriodStart() != null
                ? bill.getBillPeriodStart().format(DATE_FORMATTER) : "N/A";
        String periodEnd = bill.getBillPeriodEnd() != null
                ? bill.getBillPeriodEnd().format(DATE_FORMATTER) : "N/A";

        Optional<EnvironmentV3> envOpt = (account.getEnv() != null)
                ? envRepo.findById(account.getEnv())
                : Optional.empty();

        List<UserV2> users = userRepo.findByAccId(accId);
        Optional<UserV2> userOpt = users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));

        String userEmail = envOpt.map(EnvironmentV3::getEnvEmail)
                .filter(e -> !e.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getEmailId).orElse(null));

        String userName = envOpt.map(EnvironmentV3::getEnvCustName)
                .filter(n -> !n.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getFirstName).orElse(account.getAccName()));

        Map<String, String> templateData = new HashMap<>();
        templateData.put("accountName", account.getAccName());
        templateData.put("custName",    envOpt.map(EnvironmentV3::getEnvCustName).orElse(""));
        templateData.put("custId",      envOpt.map(EnvironmentV3::getEnvCustId).orElse(""));
        templateData.put("envCode",     envOpt.map(EnvironmentV3::getEnvCode).orElse(""));
        templateData.put("billId",      billId);
        templateData.put("periodStart", periodStart);
        templateData.put("periodEnd",   periodEnd);
        templateData.put("tokenCost",   tokenTotal.toString());
        templateData.put("compCost",    compTotal.toString());
        templateData.put("totalAmount", grandTotal.toString());

        String subject = "NNP Platform Invoice: " + billId
                + " (" + account.getAccName() + ") — Period: " + periodStart + " to " + periodEnd;

        if (userEmail != null && !userEmail.isBlank()) {
            EmailVO emailVO = new EmailVO();
            emailVO.setFrom(senderEmail);
            emailVO.setFromName("NNP Platform Billing");
            emailVO.setTo(userEmail);
            emailVO.setToName(userName);
            emailVO.setSubject(subject);
            emailVO.setTemplateName("billing_invoice.ftl");
            emailVO.setTemplateDataMap(templateData);
            try {
                mailService.sendUserWelcomeMail(emailVO);
            } catch (Exception e) {
                log.error("Failed to send invoice email for bill {}: {}", Utils.sanitizeForLog(billId), Utils.sanitizeForLog(e.getMessage()));
            }
        }

        // Log communication record
        NnpAccComm comm = new NnpAccComm();
        comm.setAccCommId(nextId("ACC_COMM"));
        comm.setNnpAccount(account);
        comm.setCommType("BILLING_INVOICE");
        comm.setCommCategory("INVOICE");
        comm.setCommTitle(subject);
        comm.setCommDescription(String.format(
                "Invoice sent for bill %s. Period: %s to %s. Token: $%s, Comp: $%s, Total: $%s",
                billId, periodStart, periodEnd, tokenTotal, compTotal, grandTotal));
        comm.setCommDate(ZonedDateTime.now());
        comm.setCreatedOn(ZonedDateTime.now());
        comm.setCreatedBy("ADMIN");
        commRepo.save(comm);

        log.info("Sent invoice for bill {} (account {}). Period: {} to {}, Total: ${}",
                Utils.sanitizeForLog(billId), Utils.sanitizeForLog(accId), Utils.sanitizeForLog(periodStart), Utils.sanitizeForLog(periodEnd), Utils.sanitizeForLog(grandTotal));
        return bill;
    }

    /**
     * Manually updates the status of a specific bill header with validated transitions.
     *
     * Allowed transitions:
     *   OPEN      → PENDING
     *   PENDING   → PAID | DISPUTED
     *   DISPUTED  → PAID
     *   Any       → CANCELLED  (admin force)
     *
     * @param billId    the acc_bill_id to update
     * @param newStatus the target status string
     * @param comment   optional admin comment recorded on the bill and in comm log
     * @param updatedBy actor/username performing the update (logged in comm record)
     */
    @Transactional
    public NnpAccBill updateBillStatus(String billId, String newStatus, String comment, String updatedBy) {
        NnpAccBill bill = billRepo.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        String currentStatus = bill.getAccBillStatus();
        validateStatusTransition(currentStatus, newStatus);

        bill.setAccBillStatus(newStatus);
        if (comment != null && !comment.isBlank()) {
            bill.setAccBillComment(comment);
        }
        billRepo.save(bill);

        // Record status change in NnpAccComm
        NnpAccount account = bill.getNnpAccount();
        if (account != null) {
            NnpAccComm comm = new NnpAccComm();
            comm.setAccCommId(nextId("ACC_COMM"));
            comm.setNnpAccount(account);
            comm.setCommType("BILLING_STATUS");
            comm.setCommCategory("STATUS_UPDATE");
            comm.setCommTitle("Bill Status Updated: " + billId);
            comm.setCommDescription(String.format(
                    "Status changed from %s → %s by %s. Comment: %s",
                    currentStatus, newStatus,
                    updatedBy != null ? updatedBy : "ADMIN",
                    comment != null ? comment : "(none)"));
            comm.setCommDate(ZonedDateTime.now(ZoneId.systemDefault()));
            comm.setCreatedOn(ZonedDateTime.now(ZoneId.systemDefault()));
            comm.setCreatedBy(updatedBy != null ? updatedBy : "ADMIN");
            commRepo.save(comm);
        }

        log.info("Bill {} status updated from {} → {} by {}", Utils.sanitizeForLog(billId), Utils.sanitizeForLog(currentStatus), Utils.sanitizeForLog(newStatus),
                Utils.sanitizeForLog(updatedBy != null ? updatedBy : "ADMIN"));
        return bill;
    }

    /**
     * Validates billing status transitions and throws IllegalStateException for invalid ones.
     */
    private void validateStatusTransition(String current, String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("New status must not be blank");
        }
        // Admin can always cancel
        if ("CANCELLED".equalsIgnoreCase(target)) return;

        boolean valid = switch (current == null ? "" : current.toUpperCase()) {
            case "OPEN"     -> "PENDING".equalsIgnoreCase(target);
            case "PENDING"  -> "PAID".equalsIgnoreCase(target) || "DISPUTED".equalsIgnoreCase(target);
            case "DISPUTED" -> "PAID".equalsIgnoreCase(target);
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException(String.format(
                    "Invalid status transition: %s → %s. " +
                    "Allowed: OPEN→PENDING, PENDING→PAID/DISPUTED, DISPUTED→PAID, Any→CANCELLED",
                    current, target));
        }
    }

    /**
     * Closes the current billing cycle at a specific closing date and opens the
     * next cycle starting from closingDate + 1.
     *
     * This gives admin full control over when a cycle ends rather than always
     * closing at "yesterday". Useful for:
     *  - mid-month manual cycle closures
     *  - back-dated closures after a data correction
     *  - starting the next bill precisely from a known date
     *
     * @param account     the account whose open bill should be closed
     * @param closingDate the last day of the billing period being closed
     */
    @Transactional
    public void closeBillingCycleFromDate(NnpAccount account, LocalDate closingDate) {
        String accId = account.getAccId();
        Optional<NnpAccBill> openBillOpt = billRepo
                .findFirstByNnpAccount_AccIdAndAccBillStatusOrderByAccBillDtDesc(accId, "OPEN");

        if (openBillOpt.isEmpty()) {
            log.warn("No open billing header found for account {} during manual cycle closure", Utils.sanitizeForLog(accId));
            return;
        }

        NnpAccBill openBill = openBillOpt.get();
        openBill.setAccBillStatus("PENDING");
        openBill.setBillPeriodEnd(closingDate);
        billRepo.save(openBill);

        // Recalculate totals from lines within the closed period
        List<NnpAccBillLn> lines = billLnRepo.findByAccBill_AccBillId(openBill.getAccBillId());
        BigDecimal totalTokenCost = BigDecimal.ZERO;
        BigDecimal totalCompCost  = BigDecimal.ZERO;

        for (NnpAccBillLn ln : lines) {
            if (ln.getTokenCost() != null) totalTokenCost = totalTokenCost.add(ln.getTokenCost());
            if (ln.getCompCost()  != null) totalCompCost  = totalCompCost.add(ln.getCompCost());
        }
        totalTokenCost = totalTokenCost.setScale(4, RoundingMode.HALF_UP);
        totalCompCost  = totalCompCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal grandTotal = totalTokenCost.add(totalCompCost).setScale(4, RoundingMode.HALF_UP);

        // Send closure email
        Optional<EnvironmentV3> envOpt = (account.getEnv() != null)
                ? envRepo.findById(account.getEnv())
                : Optional.empty();

        List<UserV2> users = userRepo.findByAccId(accId);
        Optional<UserV2> userOpt = users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));

        String userEmail = envOpt.map(EnvironmentV3::getEnvEmail)
                .filter(e -> !e.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getEmailId).orElse(null));

        String userName = envOpt.map(EnvironmentV3::getEnvCustName)
                .filter(n -> !n.isBlank())
                .orElseGet(() -> userOpt.map(UserV2::getFirstName).orElse(account.getAccName()));

        Map<String, String> templateData = new HashMap<>();
        templateData.put("accountName", account.getAccName());
        templateData.put("custName",    envOpt.map(EnvironmentV3::getEnvCustName).orElse(""));
        templateData.put("custId",      envOpt.map(EnvironmentV3::getEnvCustId).orElse(""));
        templateData.put("envCode",     envOpt.map(EnvironmentV3::getEnvCode).orElse(""));
        templateData.put("billId",       openBill.getAccBillId());
        templateData.put("periodStart",  openBill.getBillPeriodStart() != null
                ? openBill.getBillPeriodStart().format(DATE_FORMATTER) : "");
        templateData.put("periodEnd",    closingDate.format(DATE_FORMATTER));
        templateData.put("tokenCost",    totalTokenCost.toString());
        templateData.put("compCost",     totalCompCost.toString());
        templateData.put("totalAmount",  grandTotal.toString());

        String subject = "NNP Platform Invoice: " + openBill.getAccBillId()
                + " (" + account.getAccName() + ") — Manual Close";
        if (userEmail != null && !userEmail.isBlank()) {
            EmailVO emailVO = new EmailVO();
            emailVO.setFrom(senderEmail);
            emailVO.setFromName("NNP Platform Billing");
            emailVO.setTo(userEmail);
            emailVO.setToName(userName);
            emailVO.setSubject(subject);
            emailVO.setTemplateName("billing_invoice.ftl");
            emailVO.setTemplateDataMap(templateData);
            try {
                mailService.sendUserWelcomeMail(emailVO);
            } catch (Exception e) {
                log.error("Failed to send closure email for account {}: {}", Utils.sanitizeForLog(accId),Utils.sanitizeForLog( e.getMessage()));
            }
        }

        // Communication log
        NnpAccComm comm = new NnpAccComm();
        comm.setAccCommId(nextId("ACC_COMM"));
        comm.setNnpAccount(account);
        comm.setCommType("BILLING");
        comm.setCommCategory("INVOICE");
        comm.setCommTitle(subject);
        comm.setCommDescription(String.format(
                "Billing cycle manually closed at %s. Token: $%s, Comp: $%s, Total: $%s",
                closingDate, totalTokenCost, totalCompCost, grandTotal));
        comm.setCommDate(ZonedDateTime.now(ZoneId.systemDefault()));
        comm.setCreatedOn(ZonedDateTime.now(ZoneId.systemDefault()));
        comm.setCreatedBy("ADMIN");
        commRepo.save(comm);

        // Open next billing cycle starting from closingDate + 1
        LocalDate nextStart = closingDate.plusDays(1);
        NnpAccBill nextBill = new NnpAccBill();
        nextBill.setAccBillId(nextId("ACC_BILL"));
        nextBill.setNnpAccount(account);
        nextBill.setAccBillStatus("OPEN");
        nextBill.setAccBillDt(ZonedDateTime.now(ZoneId.systemDefault()));
        nextBill.setAccBillAmount(0.0f);
        nextBill.setBillPeriodStart(nextStart);
        billRepo.save(nextBill);

        log.info("Manually closed billing cycle for account {} at {}. Next cycle starts {}. Bill: {}, Total: ${}",
                Utils.sanitizeForLog(accId), Utils.sanitizeForLog(closingDate), Utils.sanitizeForLog(nextStart), Utils.sanitizeForLog(openBill.getAccBillId()), Utils.sanitizeForLog(grandTotal));
    }

    /**
     * Generates the next human-readable ID from the shared PostgreSQL sequence.
     *
     * Format examples:
     *   nextId("ACC_BILL")    → ACC_BILL_113780
     *   nextId("ACC_BILL_LN") → ACC_BILL_LN_113781
     *   nextId("ACC_COMM")    → ACC_COMM_113782
     */
    private String nextId(String prefix) {
        Long seq = billRepo.nextSequenceValue();
        return prefix + "_" + seq;
    }

    private BigDecimal parseBigDecimal(String val) {
        if (val == null || val.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
