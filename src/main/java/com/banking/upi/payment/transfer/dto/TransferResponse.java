package com.banking.upi.payment.transfer.dto;

import com.banking.upi.domain.Transaction.TxnStatus;
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
public class TransferResponse {
    private Long txnId;
    private String txnRef;
    private TxnStatus status;
    private BigDecimal amount;
    private String payerVpa;
    private String payeeVpa;
    private String note;
    private LocalDateTime createdAt;
}
