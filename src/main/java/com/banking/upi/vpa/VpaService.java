package com.banking.upi.vpa;

import com.banking.upi.domain.Account;
import com.banking.upi.domain.User;
import com.banking.upi.domain.Vpa;
import com.banking.upi.exception.ApiException;
import com.banking.upi.repository.AccountRepository;
import com.banking.upi.repository.UserRepository;
import com.banking.upi.repository.VpaRepository;
import com.banking.upi.vpa.dto.CreateVpaRequest;
import com.banking.upi.vpa.dto.VpaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpaService {

    private final VpaRepository vpaRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public VpaResponse createVpa(String mobile, CreateVpaRequest request) {
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account does not belong to this user");
        }

        if (vpaRepository.existsByHandle(request.getHandle())) {
            throw new ApiException(HttpStatus.CONFLICT, "VPA handle already exists");
        }

        Vpa vpa = Vpa.builder()
                .handle(request.getHandle())
                .userId(user.getId())
                .accountId(account.getId())
                .active(true)
                .build();

        vpa = vpaRepository.save(vpa);
        log.info("VPA created: handle={}, userId={}", vpa.getHandle(), vpa.getUserId());

        return mapToResponse(vpa);
    }

    public VpaResponse resolveVpa(String handle) {
        Vpa vpa = vpaRepository.findByHandle(handle)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VPA not found: " + handle));

        if (!vpa.getActive()) {
            throw new ApiException(HttpStatus.GONE, "VPA is inactive");
        }

        return mapToResponse(vpa);
    }

    private VpaResponse mapToResponse(Vpa vpa) {
        return VpaResponse.builder()
                .id(vpa.getId())
                .handle(vpa.getHandle())
                .userId(vpa.getUserId())
                .accountId(vpa.getAccountId())
                .active(vpa.getActive())
                .build();
    }
}
