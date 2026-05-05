package com.banking.upi.vpa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateVpaRequest {

    @NotBlank(message = "VPA handle is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$", message = "VPA handle must be in format: user@bank")
    private String handle;

    @NotNull(message = "Account ID is required")
    private Long accountId;
}
