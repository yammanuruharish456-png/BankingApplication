package com.banking.application.payments.entity;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED
}
