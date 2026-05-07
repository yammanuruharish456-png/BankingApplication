package com.banking.application.common.web;

import java.time.Instant;

public record ApiErrorResponse(String message, int status, Instant timestamp) {
}
