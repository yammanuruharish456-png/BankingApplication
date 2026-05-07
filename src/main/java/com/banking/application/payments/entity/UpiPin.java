package com.banking.application.payments.entity;

import com.banking.application.users.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "upi_pin")
public class UpiPin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String pinHash;

    @Column(nullable = false)
    private Integer failedAttempts = 0;

    private Instant lockedUntil;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
}
