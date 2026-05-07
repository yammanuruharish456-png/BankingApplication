package com.banking.application.payments.repository;

import com.banking.application.payments.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from IdempotencyKey i
            where i.user.id = :userId and i.key = :key and i.endpoint = :endpoint
            """)
    Optional<IdempotencyKey> findForUpdate(Long userId, String key, String endpoint);
}
