package com.banking.upi.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "collect_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectRequest {

    public enum CollectStatus { PENDING, APPROVED, DECLINED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_ref", nullable = false, unique = true, length = 64)
    private String requestRef;

    @Column(name = "from_vpa", nullable = false, length = 100)
    private String fromVpa;

    @Column(name = "to_vpa", nullable = false, length = 100)
    private String toVpa;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CollectStatus status = CollectStatus.PENDING;

    @Column(name = "txn_id")
    private Long txnId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

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
