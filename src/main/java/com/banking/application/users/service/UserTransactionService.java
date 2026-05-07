package com.banking.application.users.service;

import com.banking.application.payments.dto.UserTransactionDto;
import com.banking.application.payments.repository.TransactionRepository;
import com.banking.application.users.repository.VpaHandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserTransactionService {
    private final CurrentUserService currentUserService;
    private final VpaHandleRepository vpaHandleRepository;
    private final TransactionRepository transactionRepository;

    public UserTransactionService(CurrentUserService currentUserService,
                                  VpaHandleRepository vpaHandleRepository,
                                  TransactionRepository transactionRepository) {
        this.currentUserService = currentUserService;
        this.vpaHandleRepository = vpaHandleRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<UserTransactionDto> findMyTransactions() {
        Long userId = currentUserService.requireCurrentUser().getId();
        List<String> vpas = vpaHandleRepository.findAllByUserId(userId).stream().map(v -> v.getVpa()).toList();
        if (vpas.isEmpty()) {
            return List.of();
        }
        return transactionRepository.findForUserVpas(vpas).stream()
                .map(t -> new UserTransactionDto(t.getId(), t.getPayerVpa(), t.getPayeeVpa(), t.getAmount(), t.getStatus().name(), t.getNote()))
                .toList();
    }
}
