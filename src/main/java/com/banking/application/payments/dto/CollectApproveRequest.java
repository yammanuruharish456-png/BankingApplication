package com.banking.application.payments.dto;

import jakarta.validation.constraints.NotBlank;

public record CollectApproveRequest(@NotBlank String upiPin) {
}
