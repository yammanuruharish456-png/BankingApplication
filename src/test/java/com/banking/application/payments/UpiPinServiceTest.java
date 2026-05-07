package com.banking.application.payments;

import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.entity.UpiPin;
import com.banking.application.payments.repository.UpiPinRepository;
import com.banking.application.payments.service.UpiPinService;
import com.banking.application.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpiPinServiceTest {

    @Mock
    private UpiPinRepository upiPinRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UpiPinService upiPinService;

    @BeforeEach
    void init() {
        upiPinService = new UpiPinService(upiPinRepository, passwordEncoder, 3, 5);
    }

    @Test
    void verifyShouldPassForValidPin() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        UpiPin pin = new UpiPin();
        pin.setUser(user);
        pin.setPinHash("hash");

        when(upiPinRepository.findByUserId(1L)).thenReturn(Optional.of(pin));
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertDoesNotThrow(() -> upiPinService.verify(user, "1234"));
    }

    @Test
    void verifyShouldFailForInvalidPin() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 2L);
        UpiPin pin = new UpiPin();
        pin.setUser(user);
        pin.setPinHash("hash");

        when(upiPinRepository.findByUserId(2L)).thenReturn(Optional.of(pin));
        when(passwordEncoder.matches("0000", "hash")).thenReturn(false);

        assertThrows(ApiException.class, () -> upiPinService.verify(user, "0000"));
    }
}
