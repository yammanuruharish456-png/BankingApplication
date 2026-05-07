package com.banking.application.auth.service;

import com.banking.application.auth.dto.AuthResponse;
import com.banking.application.auth.dto.LoginRequest;
import com.banking.application.auth.dto.RefreshRequest;
import com.banking.application.auth.dto.RegisterRequest;
import com.banking.application.auth.entity.RefreshToken;
import com.banking.application.auth.entity.Role;
import com.banking.application.auth.repository.RefreshTokenRepository;
import com.banking.application.auth.security.JwtService;
import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.entity.UpiPin;
import com.banking.application.payments.repository.BalanceRepository;
import com.banking.application.payments.repository.UpiPinRepository;
import com.banking.application.payments.entity.Balance;
import com.banking.application.users.entity.User;
import com.banking.application.users.entity.VpaHandle;
import com.banking.application.users.repository.UserRepository;
import com.banking.application.users.repository.VpaHandleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final VpaHandleRepository vpaHandleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UpiPinRepository upiPinRepository;
    private final BalanceRepository balanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       VpaHandleRepository vpaHandleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       UpiPinRepository upiPinRepository,
                       BalanceRepository balanceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.vpaHandleRepository = vpaHandleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.upiPinRepository = upiPinRepository;
        this.balanceRepository = balanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        });
        vpaHandleRepository.findByVpa(request.vpa()).ifPresent(v -> {
            throw new ApiException(HttpStatus.CONFLICT, "VPA already exists");
        });

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user = userRepository.save(user);

        VpaHandle vpaHandle = new VpaHandle();
        vpaHandle.setVpa(request.vpa());
        vpaHandle.setUser(user);
        vpaHandleRepository.save(vpaHandle);

        UpiPin upiPin = new UpiPin();
        upiPin.setUser(user);
        upiPin.setPinHash(passwordEncoder.encode(request.upiPin()));
        upiPinRepository.save(upiPin);

        Balance balance = new Balance();
        balance.setAccount(user);
        balanceRepository.save(balance);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        return issueTokens(refreshToken.getUser());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }
}
