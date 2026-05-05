package com.banking.upi.pin;

import com.banking.upi.domain.UpiPin;
import com.banking.upi.domain.User;
import com.banking.upi.exception.ApiException;
import com.banking.upi.pin.dto.SetPinRequest;
import com.banking.upi.repository.UpiPinRepository;
import com.banking.upi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PinService {

    private final UpiPinRepository upiPinRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${upi.pin.max-failed-attempts:3}")
    private int maxFailedAttempts;

    @Value("${upi.pin.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Transactional
    public void setPin(String mobile, SetPinRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String hashedPin = passwordEncoder.encode(request.getPin());

        Optional<UpiPin> existingPin = upiPinRepository.findByUserId(user.getId());
        if (existingPin.isPresent()) {
            UpiPin pin = existingPin.get();
            pin.setPinHash(hashedPin);
            pin.setFailedAttempts(0);
            pin.setLockedUntil(null);
            upiPinRepository.save(pin);
            log.info("PIN updated for userId={}", user.getId());
        } else {
            UpiPin pin = UpiPin.builder()
                    .userId(user.getId())
                    .pinHash(hashedPin)
                    .failedAttempts(0)
                    .build();
            upiPinRepository.save(pin);
            log.info("PIN set for userId={}", user.getId());
        }
    }

    @Transactional
    public void verifyPin(Long userId, String rawPin) {
        UpiPin upiPin = upiPinRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "UPI PIN not set"));

        if (upiPin.getLockedUntil() != null && LocalDateTime.now().isBefore(upiPin.getLockedUntil())) {
            throw new ApiException(HttpStatus.LOCKED, "Account is locked. Try after " + upiPin.getLockedUntil());
        }

        if (!passwordEncoder.matches(rawPin, upiPin.getPinHash())) {
            int attempts = upiPin.getFailedAttempts() + 1;
            upiPin.setFailedAttempts(attempts);

            if (attempts >= maxFailedAttempts) {
                upiPin.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                upiPinRepository.save(upiPin);
                throw new ApiException(HttpStatus.LOCKED, "Too many failed attempts. Account locked for " + lockoutDurationMinutes + " minutes.");
            }

            upiPinRepository.save(upiPin);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid UPI PIN. " + (maxFailedAttempts - attempts) + " attempts remaining.");
        }

        // Reset on success
        if (upiPin.getFailedAttempts() > 0) {
            upiPin.setFailedAttempts(0);
            upiPin.setLockedUntil(null);
            upiPinRepository.save(upiPin);
        }
    }

    @Transactional
    public void resetLockout(Long userId) {
        upiPinRepository.findByUserId(userId).ifPresent(pin -> {
            pin.setFailedAttempts(0);
            pin.setLockedUntil(null);
            upiPinRepository.save(pin);
        });
    }
}
