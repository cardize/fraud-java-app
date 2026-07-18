package com.fraud.api.security;

import com.fraud.infrastructure.persistence.RefreshToken;
import com.fraud.infrastructure.persistence.RefreshTokenJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Refresh tokens with ROTATION and REUSE DETECTION.
 *
 * Why not just a longer-lived JWT? A long-lived bearer token cannot be revoked and a stolen one
 * works until expiry. Server-side opaque tokens give us:
 *  - revocation (logout kills them instantly),
 *  - one-shot rotation (each refresh consumes the token and issues a fresh one),
 *  - theft detection: if a CONSUMED token arrives again, someone is replaying it (the legitimate
 *    client already holds the rotated successor) — the response is to revoke ALL of that user's
 *    refresh tokens, forcing an interactive re-login for attacker and victim alike.
 *
 * The raw token is 256 bits from SecureRandom, handed to the client base64url-encoded; the DB
 * stores only its SHA-256 hash.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenJpaRepository repository;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenJpaRepository repository,
                               @Value("${fraud.security.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs; // default: 7 days
    }

    /** Issues a new refresh token for the user and returns the RAW value (shown only once). */
    @Transactional
    public String issue(String username) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(new RefreshToken(sha256(raw), username,
                Instant.now().plusMillis(refreshExpirationMs)));
        return raw;
    }

    /**
     * Consumes (one-shot) a refresh token; returns the username when valid.
     * Empty when: unknown, expired, or ALREADY USED — the last case additionally revokes all of
     * the user's refresh tokens (rotation-reuse = theft signal).
     *
     * RACE FIX (external review finding C): the used-check and the marking are ONE conditional
     * UPDATE ({@link RefreshTokenJpaRepository#markUsedIfUnused}) instead of the old
     * read-isUsed-then-markUsed sequence. Two concurrent /refresh calls with the same token could
     * previously BOTH read used=false and both succeed, silently skipping reuse detection. Now
     * exactly one wins the UPDATE; the loser is treated the same as any reuse — family revoked.
     * (Strict by design: even a legitimate client double-firing refresh loses its family and must
     * log in again — indistinguishable from theft, so treated as such.)
     */
    @Transactional
    public Optional<String> consume(String rawToken) {
        String hash = sha256(rawToken);
        RefreshToken token = repository.findByTokenHash(hash).orElse(null);
        if (token == null) {
            return Optional.empty();
        }
        if (token.isExpired(Instant.now())) {
            return Optional.empty();
        }
        if (repository.markUsedIfUnused(hash) == 0) {
            log.warn("Refresh token REUSE detected for user '{}' — revoking all refresh tokens", token.getUsername());
            repository.deleteByUsername(token.getUsername());
            return Optional.empty();
        }
        return Optional.of(token.getUsername());
    }

    /** Revokes all of the user's refresh tokens (logout). */
    @Transactional
    public void revokeAll(String username) {
        repository.deleteByUsername(username);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // mandated by the JVM spec
        }
    }
}
