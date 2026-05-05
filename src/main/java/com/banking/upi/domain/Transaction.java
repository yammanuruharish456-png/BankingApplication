package com.banking.upi.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    public enum TxnStatus { PENDING, SUCCESS, FAILED }
    public enum TxnType { PAY, COLLECT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 64)
    private String txnRef;

    @Column(name = "payer_vpa", nullable = false, length = 100)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false, length = 100)
    private String payeeVpa;

    @Column(name = "payer_account_id", nullable = false)
    private Long payerAccountId;

    @Column(name = "payee_account_id", nullable = false)
    private Long payeeAccountId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TxnStatus status = TxnStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    @Builder.Default
    private TxnType txnType = TxnType.PAY;

    @Column(name = "client_ref", length = 100)
    private String clientRef;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
