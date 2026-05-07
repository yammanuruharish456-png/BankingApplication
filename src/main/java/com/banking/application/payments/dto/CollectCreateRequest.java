package com.banking.application.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CollectCreateRequest(
        @NotBlank String fromVpa,
        @NotBlank String toVpa,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String note
) {
}
