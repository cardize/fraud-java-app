package com.fraud.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Holds the jtis (JWT IDs) of revoked (logged-out) tokens.
 *
 * Stateless JWT inherently has no "logout" — a token stays valid until it expires. This blacklist
 * lets a token be invalidated early when the user explicitly logs out. Only the jti is stored,
 * NOT the whole token (tiny memory footprint); an entry's TTL equals the longest possible token
 * lifetime — keeping it any longer would be pointless anyway.
 *
 * SCOPE (single-instance, accepted trade-off — external review finding D): the blacklist is
 * in-process. In a horizontally scaled deployment a logout on instance A does not revoke the
 * token on instance B, and a restart forgets revocations. The blast radius is bounded by the
 * access-token TTL (refresh tokens are DB-backed and revoke globally); before scaling out, move
 * this to a shared store (Redis) keyed by jti with the same TTL.
 */
@Component
public class TokenBlacklist {

    private final Cache<String, Boolean> revoked;

    public TokenBlacklist(@Value("${fraud.security.jwt-expiration-ms:3600000}") long jwtExpirationMs) {
        this.revoked = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(Duration.ofMillis(jwtExpirationMs))
                .build();
    }

    public void revoke(String jti) {
        if (jti != null) {
            revoked.put(jti, Boolean.TRUE);
        }
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.getIfPresent(jti) != null;
    }
}
