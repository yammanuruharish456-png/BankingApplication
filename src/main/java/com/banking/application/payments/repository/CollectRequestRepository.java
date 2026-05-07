package com.banking.application.payments.repository;

import com.banking.application.payments.entity.CollectRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectRequestRepository extends JpaRepository<CollectRequest, Long> {
}
