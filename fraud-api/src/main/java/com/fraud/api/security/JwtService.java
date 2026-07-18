package com.fraud.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and validates JWTs (HMAC-SHA256, symmetric secret key).
 *
 * A symmetric key needs no IdP; production would move to an IdP/JWKS.
 * Every token carries a unique jti (JWT ID) — used to blacklist just the jti on logout instead
 * of storing the whole token (see {@link TokenBlacklist}).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${fraud.security.jwt-secret}") String secret,
                      @Value("${fraud.security.jwt-expiration-ms:3600000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Issues a token carrying the user's roles (RBAC) and TENANT as claims. Roles are read back by
     * {@link JwtAuthenticationFilter} and turned into Spring authorities — the token is
     * self-contained, no DB lookup is needed per request.
     *
     * SECURITY (cross-tenant isolation): the tenant the user logged in under is BOUND to the
     * token. Without this claim, a token issued for tenant "alpha" could be replayed with an
     * "X-Tenant: beta" header and the request would be routed to beta's database — the header
     * alone proved nothing about the caller. JwtAuthenticationFilter rejects any request whose
     * X-Tenant context doesn't match the token's tenant.
     */
    public String issue(String username, Collection<String> roles, String tenant) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("roles", List.copyOf(roles))
                .claim("tenant", tenant)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Returns the claims if the token is valid (signature + expiry); null otherwise. */
    public Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
