package com.banking.application.payments.entity;

import com.banking.application.users.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private User account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public void setAccount(User account) { this.account = account; }
    public void setEntryType(LedgerEntryType entryType) { this.entryType = entryType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
