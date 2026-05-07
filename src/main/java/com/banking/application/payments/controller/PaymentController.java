package com.banking.application.payments.controller;

import com.banking.application.payments.dto.TransferRequest;
import com.banking.application.payments.dto.TransferResponse;
import com.banking.application.payments.service.TransferService;
import com.banking.application.users.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {
    private final TransferService transferService;
    private final CurrentUserService currentUserService;

    public PaymentController(TransferService transferService, CurrentUserService currentUserService) {
        this.transferService = transferService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(currentUserService.requireCurrentUser(), idempotencyKey, request);
    }
}
