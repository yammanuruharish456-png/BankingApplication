package com.banking.upi.payment.transfer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Payer VPA is required")
    private String payerVpa;

    @NotBlank(message = "Payee VPA is required")
    private String payeeVpa;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "100000.00", message = "Amount cannot exceed 100000.00")
    private BigDecimal amount;

    @Size(max = 255)
    private String note;

    @NotBlank(message = "UPI PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "UPI PIN must be 4-6 digits")
    private String upiPin;

    @Size(max = 100)
    private String clientRef;
}
