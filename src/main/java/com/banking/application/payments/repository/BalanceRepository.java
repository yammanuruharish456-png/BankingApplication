package com.banking.application.payments.repository;

import com.banking.application.payments.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Balance b where b.account.id = :accountId")
    Optional<Balance> findByAccountIdForUpdate(Long accountId);

    Optional<Balance> findByAccountId(Long accountId);

    List<Balance> findAllByAccountIdIn(List<Long> accountIds);
}
