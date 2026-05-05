package com.banking.upi.payment.collect.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CollectRequestDto {

    @NotBlank(message = "From VPA is required")
    private String fromVpa;

    @NotBlank(message = "To VPA is required")
    private String toVpa;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "100000.00", message = "Amount cannot exceed 100000.00")
    private BigDecimal amount;

    @Size(max = 255)
    private String note;
}
