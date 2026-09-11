package com.nnp.dashboard.controller;

import com.nnp.dashboard.model.NnpAccBill;
import com.nnp.dashboard.model.NnpAccBillLn;
import com.nnp.dashboard.model.NnpAccComm;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.repo.NnpAccBillLnRepo;
import com.nnp.dashboard.repo.NnpAccBillRepo;
import com.nnp.dashboard.repo.NnpAccCommunicationRepo;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.service.BillingService;
import com.nnp.dashboard.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/admin/billing", "/accounts/billing"})
@RequiredArgsConstructor
@Slf4j
public class BillingAdminController {

    private final NnpAccountRepo accountRepo;
    private final NnpAccBillRepo billRepo;
    private final NnpAccBillLnRepo billLnRepo;
    private final NnpAccCommunicationRepo commRepo;
    private final BillingService billingService;

    /**
     * View all bill headers for a given account.
     */
    @GetMapping("/bills/{accId}")
    public List<NnpAccBill> getBillsForAccount(@PathVariable String accId) {
        return billRepo.findByNnpAccount_AccIdOrderByAccBillDtDesc(accId);
    }

    /**
     * View daily bill line items for a bill header.
     */
    @GetMapping("/bill-lines/{billId}")
    public List<NnpAccBillLn> getBillLinesForBill(@PathVariable String billId) {
        return billLnRepo.findByAccBill_AccBillId(billId);
    }

    /**
     * View all billing communications for an account.
     */
    @GetMapping("/comms/{accId}")
    public List<NnpAccComm> getCommunicationsForAccount(@PathVariable String accId) {
        return commRepo.findByNnpAccount_AccIdOrderByCommDateDesc(accId);
    }

    /**
     * Admin manual trigger to calculate total bill from date range and send email.
     * @deprecated Use POST /bills/{billId}/send-invoice instead — which sends invoice
     *             using the bill's own recorded period start/end dates.
     */

    @PostMapping("/{accId}/send-bill")
    public Map<String, String> sendBillManually(
            @PathVariable String accId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        log.info("Admin manually sending bill for account {} from {} to {}", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(fromDate), Utils.sanitizeForLog(toDate));
        billingService.sendBillManually(accId, fromDate, toDate);
        return Map.of("status", "SUCCESS", "message", "Bill email sent successfully for period " + fromDate + " to " + toDate);
    }

    /**
     * Send invoice email for a specific bill header.
     *
     * Uses the bill's own billPeriodStart and billPeriodEnd dates and sums
     * the line items that are directly linked to this bill — so the email
     * period exactly matches the recorded billing period.
     *
     * POST /admin/billing/bills/{billId}/send-invoice
     */
    @PostMapping("/bills/{billId}/send-invoice")
    public ResponseEntity<?> sendInvoiceForBill(@PathVariable String billId) {
        log.info("Admin sending invoice for bill {}", Utils.sanitizeForLog(billId));
        try {
            NnpAccBill bill = billingService.sendInvoiceForBill(billId);
            return ResponseEntity.ok(Map.of(
                    "status",      "SUCCESS",
                    "message",     "Invoice email sent for bill " + billId,
                    "billId",      bill.getAccBillId(),
                    "periodStart", bill.getBillPeriodStart() != null ? bill.getBillPeriodStart().toString() : "N/A",
                    "periodEnd",   bill.getBillPeriodEnd()   != null ? bill.getBillPeriodEnd().toString()   : "N/A"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /**
     * Admin manual trigger to run daily bill calculation for an account.
     */
    @PostMapping("/{accId}/run-daily")
    public Map<String, String> runDailyBillManually(
            @PathVariable String accId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billingDate
    ) {
        LocalDate targetDate = billingDate != null ? billingDate : LocalDate.now().minusDays(1);
        NnpAccount account = accountRepo.findById(accId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accId));

        log.info("Admin manually running daily billing for account {} on {}", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(targetDate));
        billingService.generateDailyBillForAccount(account, targetDate);
        return Map.of("status", "SUCCESS", "message", "Daily bill generated successfully for date " + targetDate);
    }

    /**
     * Manually update the status of a specific bill header.
     *
     * Validates allowed transitions:
     *   OPEN → PENDING, PENDING → PAID|DISPUTED, DISPUTED → PAID, Any → CANCELLED
     *
     * Body: { "status": "PAID", "comment": "optional admin note" }
     */
    @PatchMapping("/bills/{billId}/status")
    public ResponseEntity<?> updateBillStatus(
            @PathVariable String billId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Name", required = false) String updatedBy
    ) {
        String newStatus = body.get("status");
        String comment   = body.get("comment");

        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "ERROR", "message", "Field 'status' is required in request body"));
        }

        log.info("Admin updating bill {} status to '{}' — requested by {}", Utils.sanitizeForLog(billId), Utils.sanitizeForLog(newStatus), Utils.sanitizeForLog(updatedBy));
        try {
            NnpAccBill updated = billingService.updateBillStatus(billId, newStatus.toUpperCase(), comment, updatedBy);
            return ResponseEntity.ok(Map.of(
                    "status",    "SUCCESS",
                    "message",   "Bill status updated to " + newStatus.toUpperCase(),
                    "billId",    updated.getAccBillId(),
                    "newStatus", updated.getAccBillStatus()
            ));
        } catch (IllegalStateException e) {
            // Invalid transition
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Bill not found
            return ResponseEntity.status(404)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    /**
     * Close the current OPEN billing cycle at a specific date and open the next
     * cycle starting from closingDate + 1.
     *
     * Example: POST /admin/billing/ACC-001/close-cycle?closingDate=2026-07-31
     *   → closes bill with billPeriodEnd = 2026-07-31
     *   → opens next bill with billPeriodStart = 2026-08-01
     */
    @PostMapping("/{accId}/close-cycle")
    public ResponseEntity<?> closeBillingCycleFromDate(
            @PathVariable String accId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate
    ) {
        NnpAccount account = accountRepo.findById(accId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accId));

        log.info("Admin manually closing billing cycle for account {} at closing date {}", Utils.sanitizeForLog(accId), Utils.sanitizeForLog(closingDate));
        billingService.closeBillingCycleFromDate(account, closingDate);

        return ResponseEntity.ok(Map.of(
                "status",         "SUCCESS",
                "message",        "Billing cycle closed at " + closingDate + ". Next cycle starts " + closingDate.plusDays(1),
                "closedAt",       closingDate.toString(),
                "nextCycleStart", closingDate.plusDays(1).toString()
        ));
    }
}
