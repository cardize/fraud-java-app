package com.fraud.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Server-side refresh token record.
 *
 * Holds only the SHA-256 HASH of the token — the raw value exists solely in the client's hands,
 * so a leaked DB dump contains nothing replayable. `used` implements one-shot rotation: a refresh
 * token is consumed exactly once; a second arrival of the same token is treated as theft
 * (see RefreshTokenService).
 */
@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(columnNames = "token_hash"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;
    private String username;
    private Instant expiresAt;
    private boolean used;
    private Instant createdAt;

    protected RefreshToken() {
        // no-arg constructor required by JPA
    }

    public RefreshToken(String tokenHash, String username, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.username = username;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = Instant.now();
    }

    public String getUsername() { return username; }
    public boolean isUsed() { return used; }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void markUsed() {
        this.used = true;
    }
}
