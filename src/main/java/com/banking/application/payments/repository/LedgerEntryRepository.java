package com.banking.application.payments.repository;

import com.banking.application.payments.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    @Query("""
            select le.account.id,
                   sum(case when le.entryType = com.banking.application.payments.entity.LedgerEntryType.CREDIT then le.amount else -le.amount end)
            from LedgerEntry le
            group by le.account.id
            """)
    List<Object[]> sumByAccount();
}
