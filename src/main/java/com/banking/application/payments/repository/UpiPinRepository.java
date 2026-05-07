package com.banking.application.payments.repository;

import com.banking.application.payments.entity.UpiPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UpiPinRepository extends JpaRepository<UpiPin, Long> {
    Optional<UpiPin> findByUserId(Long userId);
}
