package com.nnp.dashboard.scheduler;

import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.service.BillingService;
import com.nnp.dashboard.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.billing.cycle.enabled", havingValue = "true", matchIfMissing = true)
public class BillingCycleScheduler {

    private final NnpAccountRepo accountRepo;
    private final BillingService billingService;

    /**
     * Runs daily at 12:05 AM to check if today matches any account's creation day of month (billing cycle end).
     */
    @Scheduled(cron = "${scheduler.billing.cycle.cron:0 5 0 * * *}")
    public void runBillingCycleJob() {
        log.info("=== BILLING CYCLE SCHEDULER STARTED ===");
        int todayDayOfMonth = LocalDate.now().getDayOfMonth();

        List<NnpAccount> activeAccounts = accountRepo.findAll();
        int closedCount = 0;

        for (NnpAccount account : activeAccounts) {
            if (account.getCreatedOn() != null && account.getCreatedOn().getDayOfMonth() == todayDayOfMonth) {
                try {
                    log.info("-----Account {} billing date arrived (day {}), closing cycle and sending bill email",
                            Utils.sanitizeForLog(account.getAccId()), Utils.sanitizeForLog(todayDayOfMonth));
                    billingService.closeBillingCycle(account);
                    closedCount++;
                } catch (Exception e) {
                    log.error("Error closing billing cycle for account {}: {}", Utils.sanitizeForLog(account.getAccId()), Utils.sanitizeForLog(e.getMessage()), Utils.sanitizeForLog(e));
                }
            }
        }
        log.info("=== BILLING CYCLE SCHEDULER FINISHED. Closed {} cycles ===", closedCount);
    }
}
