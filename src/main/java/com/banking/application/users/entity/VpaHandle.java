package com.banking.application.users.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vpa_handles")
public class VpaHandle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vpa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    public Long getId() { return id; }
    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
