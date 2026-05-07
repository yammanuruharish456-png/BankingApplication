package com.banking.application.payments.service;

import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.entity.UpiPin;
import com.banking.application.payments.repository.UpiPinRepository;
import com.banking.application.users.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UpiPinService {
    private final UpiPinRepository upiPinRepository;
    private final PasswordEncoder passwordEncoder;
    private final int maxAttempts;
    private final int cooldownMinutes;

    public UpiPinService(UpiPinRepository upiPinRepository,
                         PasswordEncoder passwordEncoder,
                         @Value("${app.upi.max-failed-attempts:3}") int maxAttempts,
                         @Value("${app.upi.cooldown-minutes:5}") int cooldownMinutes) {
        this.upiPinRepository = upiPinRepository;
        this.passwordEncoder = passwordEncoder;
        this.maxAttempts = maxAttempts;
        this.cooldownMinutes = cooldownMinutes;
    }

    @Transactional
    public void verify(User user, String upiPin) {
        UpiPin pin = upiPinRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "UPI PIN not set"));
        Instant now = Instant.now();
        if (pin.getLockedUntil() != null && pin.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "UPI PIN temporarily locked");
        }

        if (!passwordEncoder.matches(upiPin, pin.getPinHash())) {
            int attempts = pin.getFailedAttempts() + 1;
            pin.setFailedAttempts(attempts);
            if (attempts >= maxAttempts) {
                pin.setFailedAttempts(0);
                pin.setLockedUntil(now.plus(cooldownMinutes, ChronoUnit.MINUTES));
            }
            upiPinRepository.save(pin);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid UPI PIN");
        }

        pin.setFailedAttempts(0);
        pin.setLockedUntil(null);
        upiPinRepository.save(pin);
    }
}
