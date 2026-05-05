package com.banking.upi.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private Long userId;
    private String accountNumber;
    private String ifsc;
    private String bankName;
    private BigDecimal balance;
    private Boolean enabled;
}
