package com.banking.upi.repository;

import com.banking.upi.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTxnRef(String txnRef);
    Page<Transaction> findByPayerVpaOrPayeeVpa(String payerVpa, String payeeVpa, Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
