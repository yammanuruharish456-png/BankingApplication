package com.banking.application.payments.reconciliation;

import com.banking.application.payments.entity.AuditLog;
import com.banking.application.payments.repository.AuditLogRepository;
import com.banking.application.payments.repository.BalanceRepository;
import com.banking.application.payments.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceRepository balanceRepository;
    private final AuditLogRepository auditLogRepository;

    public ReconciliationJob(LedgerEntryRepository ledgerEntryRepository,
                             BalanceRepository balanceRepository,
                             AuditLogRepository auditLogRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.balanceRepository = balanceRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(fixedDelayString = "${app.reconciliation.delay-ms:60000}")
    public void reconcile() {
        Map<Long, BigDecimal> ledgerSums = ledgerEntryRepository.sumByAccount().stream()
                .collect(Collectors.toMap(o -> (Long) o[0], o -> (BigDecimal) o[1]));

        balanceRepository.findAll().forEach(balance -> {
            BigDecimal ledgerValue = ledgerSums.getOrDefault(balance.getAccount().getId(), BigDecimal.ZERO);
            if (ledgerValue.compareTo(balance.getAmount()) != 0) {
                String details = "accountId=" + balance.getAccount().getId() + ", balance=" + balance.getAmount() + ", ledger=" + ledgerValue;
                log.warn("reconciliation_mismatch {}", details);
                AuditLog auditLog = new AuditLog();
                auditLog.setEventType("RECONCILIATION_MISMATCH");
                auditLog.setDetails(details);
                auditLogRepository.save(auditLog);
            }
        });
    }
}
