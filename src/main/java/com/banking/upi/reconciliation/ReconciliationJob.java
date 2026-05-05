package com.banking.upi.reconciliation;

import com.banking.upi.domain.Account;
import com.banking.upi.domain.Balance;
import com.banking.upi.repository.AccountRepository;
import com.banking.upi.repository.BalanceRepository;
import com.banking.upi.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationJob {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    // Runs every hour at minute 0 (e.g., 01:00, 02:00, ...)
    @Scheduled(cron = "0 0 * * * *")
    public void reconcile() {
        log.info("Starting hourly reconciliation job...");
        List<Account> accounts = accountRepository.findAll();
        int discrepancies = 0;

        for (Account account : accounts) {
            try {
                Optional<Balance> balanceOpt = balanceRepository.findByAccountId(account.getId());
                if (balanceOpt.isEmpty()) {
                    log.warn("No balance row found for accountId={}", account.getId());
                    continue;
                }

                Balance balance = balanceOpt.get();
                BigDecimal ledgerNet = ledgerEntryRepository.findNetBalanceByAccountId(account.getId());

                if (ledgerNet == null) {
                    ledgerNet = BigDecimal.ZERO;
                }

                BigDecimal difference = balance.getAmount().subtract(ledgerNet).abs();
                if (difference.compareTo(BigDecimal.ZERO) > 0) {
                    log.warn("RECONCILIATION DISCREPANCY: accountId={}, storedBalance={}, ledgerNet={}, diff={}",
                            account.getId(), balance.getAmount(), ledgerNet, difference);
                    discrepancies++;
                }
            } catch (Exception e) {
                log.error("Error during reconciliation for accountId={}: {}", account.getId(), e.getMessage());
            }
        }

        log.info("Reconciliation complete. Accounts checked={}, discrepancies={}", accounts.size(), discrepancies);
    }
}
