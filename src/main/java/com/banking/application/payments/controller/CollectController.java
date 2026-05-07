package com.banking.application.payments.controller;

import com.banking.application.payments.dto.*;
import com.banking.application.payments.service.CollectService;
import com.banking.application.users.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/collect")
public class CollectController {
    private final CollectService collectService;
    private final CurrentUserService currentUserService;

    public CollectController(CollectService collectService, CurrentUserService currentUserService) {
        this.collectService = collectService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public CollectResponse create(@Valid @RequestBody CollectCreateRequest request) {
        return collectService.create(currentUserService.requireCurrentUser(), request);
    }

    @PostMapping("/{requestId}/approve")
    public TransferResponse approve(@PathVariable Long requestId,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody CollectApproveRequest request) {
        return collectService.approve(currentUserService.requireCurrentUser(), requestId, idempotencyKey, request);
    }

    @PostMapping("/{requestId}/decline")
    public CollectResponse decline(@PathVariable Long requestId) {
        return collectService.decline(currentUserService.requireCurrentUser(), requestId);
    }
}
