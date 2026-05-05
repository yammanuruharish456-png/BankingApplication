package com.banking.upi.repository;

import com.banking.upi.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByTxnId(Long txnId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.accountId = :accountId")
    BigDecimal findNetBalanceByAccountId(@Param("accountId") Long accountId);
}
