package com.banking.application.payments.repository;

import com.banking.application.payments.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("""
            select t from Transaction t
            where t.payerVpa in ?1 or t.payeeVpa in ?1
            order by t.id desc
            """)
    List<Transaction> findForUserVpas(List<String> vpas);
}
