package com.banking.application.payments.dto;

import java.math.BigDecimal;

public record UserTransactionDto(Long transactionId, String payerVpa, String payeeVpa, BigDecimal amount, String status, String note) {
}
