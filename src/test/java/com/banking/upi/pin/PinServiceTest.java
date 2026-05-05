package com.banking.upi.pin;

import com.banking.upi.domain.UpiPin;
import com.banking.upi.domain.User;
import com.banking.upi.exception.ApiException;
import com.banking.upi.pin.dto.SetPinRequest;
import com.banking.upi.repository.UpiPinRepository;
import com.banking.upi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PinServiceTest {

    @Mock private UpiPinRepository upiPinRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private PinService pinService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pinService, "maxFailedAttempts", 3);
        ReflectionTestUtils.setField(pinService, "lockoutDurationMinutes", 30);

        user = User.builder()
                .id(1L)
                .mobile("9876543210")
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hash")
                .enabled(true)
                .build();
    }

    @Test
    void setPin_newPin_success() {
        SetPinRequest request = new SetPinRequest();
        request.setPin("1234");

        when(userRepository.findByMobile(any())).thenReturn(Optional.of(user));
        when(upiPinRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPin");
        when(upiPinRepository.save(any())).thenReturn(UpiPin.builder().build());

        assertThatNoException().isThrownBy(() -> pinService.setPin("9876543210", request));
        verify(upiPinRepository).save(any());
    }

    @Test
    void verifyPin_correct_success() {
        UpiPin upiPin = UpiPin.builder()
                .userId(1L)
                .pinHash("hashedPin")
                .failedAttempts(0)
                .build();

        when(upiPinRepository.findByUserId(1L)).thenReturn(Optional.of(upiPin));
        when(passwordEncoder.matches("1234", "hashedPin")).thenReturn(true);

        assertThatNoException().isThrownBy(() -> pinService.verifyPin(1L, "1234"));
    }

    @Test
    void verifyPin_wrongPin_incrementsFailedAttempts() {
        UpiPin upiPin = UpiPin.builder()
                .userId(1L)
                .pinHash("hashedPin")
                .failedAttempts(0)
                .build();

        when(upiPinRepository.findByUserId(1L)).thenReturn(Optional.of(upiPin));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(upiPinRepository.save(any())).thenReturn(upiPin);

        assertThatThrownBy(() -> pinService.verifyPin(1L, "wrong"))
                .isInstanceOf(ApiException.class);

        assertThat(upiPin.getFailedAttempts()).isEqualTo(1);
    }

    @Test
    void verifyPin_lockedAccount_throwsLocked() {
        UpiPin upiPin = UpiPin.builder()
                .userId(1L)
                .pinHash("hashedPin")
                .failedAttempts(3)
                .lockedUntil(LocalDateTime.now().plusMinutes(25))
                .build();

        when(upiPinRepository.findByUserId(1L)).thenReturn(Optional.of(upiPin));

        assertThatThrownBy(() -> pinService.verifyPin(1L, "1234"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.LOCKED));
    }

    @Test
    void verifyPin_maxFailedAttempts_locksAccount() {
        UpiPin upiPin = UpiPin.builder()
                .userId(1L)
                .pinHash("hashedPin")
                .failedAttempts(2)
                .build();

        when(upiPinRepository.findByUserId(1L)).thenReturn(Optional.of(upiPin));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(upiPinRepository.save(any())).thenReturn(upiPin);

        assertThatThrownBy(() -> pinService.verifyPin(1L, "wrong"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.LOCKED));

        assertThat(upiPin.getLockedUntil()).isNotNull();
    }
}
