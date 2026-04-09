package com.musicplayer.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt = Instant.now().plusSeconds(30L * 24 * 60 * 60); // 30 days

    // ── Getters & Setters ─────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public void setId(UUID id)           { this.id = id; }

    public User getUser()                { return user; }
    public void setUser(User user)       { this.user = user; }

    public String getToken()             { return token; }
    public void setToken(String token)   { this.token = token; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant createdAt)  { this.createdAt = createdAt; }

    public Instant getExpiresAt()                { return expiresAt; }
    public void setExpiresAt(Instant expiresAt)  { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}