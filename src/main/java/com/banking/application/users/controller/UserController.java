package com.banking.application.users.controller;

import com.banking.application.payments.dto.UserTransactionDto;
import com.banking.application.users.service.UserTransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/users/me")
public class UserController {
    private final UserTransactionService userTransactionService;

    public UserController(UserTransactionService userTransactionService) {
        this.userTransactionService = userTransactionService;
    }

    @GetMapping("/transactions")
    public List<UserTransactionDto> transactions() {
        return userTransactionService.findMyTransactions();
    }
}
