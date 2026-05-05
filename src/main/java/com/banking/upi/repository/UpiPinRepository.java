package com.banking.upi.repository;

import com.banking.upi.domain.UpiPin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UpiPinRepository extends JpaRepository<UpiPin, Long> {
    Optional<UpiPin> findByUserId(Long userId);
}
