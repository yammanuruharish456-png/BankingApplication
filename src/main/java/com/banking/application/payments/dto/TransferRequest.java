package com.banking.application.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String payerVpa,
        @NotBlank String payeeVpa,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String note,
        @NotBlank String upiPin,
        @NotBlank String clientRef
) {
}
