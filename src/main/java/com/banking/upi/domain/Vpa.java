package com.banking.upi.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vpas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String handle;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
