package com.banking.application.payments.repository;

import com.banking.application.payments.entity.OutboxEvent;
import com.banking.application.payments.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
}
