package com.banking.application.payments.entity;

import com.banking.application.users.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idempotency_key", "endpoint"}))
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private String requestHash;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public IdempotencyStatus getStatus() { return status; }
    public void setStatus(IdempotencyStatus status) { this.status = status; }
}
