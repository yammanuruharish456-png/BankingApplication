package com.banking.upi.payment.collect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApproveRequest {

    @NotBlank(message = "UPI PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "UPI PIN must be 4-6 digits")
    private String upiPin;
}
