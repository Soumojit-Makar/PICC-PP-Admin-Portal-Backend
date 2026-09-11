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
import java.time.ZoneId;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.billing.daily.enabled", havingValue = "true", matchIfMissing = true)
public class DailyBillingScheduler {

    private final NnpAccountRepo accountRepo;
    private final BillingService billingService;

    /**
     * Runs daily at midnight to calculate yesterday's token and active component usage bills.
     */
    @Scheduled(cron = "${scheduler.billing.daily.cron:0 0 0 * * *}")
    public void runDailyBillingJob() {
        log.info("=== DAILY BILLING SCHEDULER STARTED ===");
        LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        List<NnpAccount> activeAccounts = accountRepo.findAll();

        log.info("Processing daily billing for {} active accounts for date {}", Utils.sanitizeForLog(activeAccounts.size()), Utils.sanitizeForLog(yesterday));
        for (NnpAccount account : activeAccounts) {
            try {
                billingService.generateDailyBillForAccount(account, yesterday);
            } catch (Exception e) {
                log.error("Error generating daily bill for account {}: {}", Utils.sanitizeForLog(account.getAccId()), Utils.sanitizeForLog(e.getMessage()), Utils.sanitizeForLog(e));
            }
        }
        log.info("=== DAILY BILLING SCHEDULER FINISHED ===");
    }
}
