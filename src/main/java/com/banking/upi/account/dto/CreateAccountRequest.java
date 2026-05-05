package com.banking.upi.account.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account number is required")
    @Size(max = 20)
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Size(max = 20)
    private String ifsc;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    private String bankName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;
}
