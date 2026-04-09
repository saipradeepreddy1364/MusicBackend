package com.musicplayer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;   // BCrypt hashed

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    // ── Getters & Setters ─────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public void setId(UUID id)           { this.id = id; }

    public String getEmail()             { return email; }
    public void setEmail(String email)   { this.email = email; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant createdAt)  { this.createdAt = createdAt; }
}