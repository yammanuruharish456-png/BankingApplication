package com.banking.upi.payment.collect.dto;

import com.banking.upi.domain.CollectRequest.CollectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectResponse {
    private Long requestId;
    private String requestRef;
    private String fromVpa;
    private String toVpa;
    private BigDecimal amount;
    private String note;
    private CollectStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
