package com.banking.application.payments.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setDetails(String details) { this.details = details; }
}
