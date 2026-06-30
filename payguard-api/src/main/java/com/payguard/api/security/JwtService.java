package com.payguard.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT üretir ve doğrular (HMAC-SHA256, simetrik gizli anahtar).
 *
 * Simetrik anahtar IdP gerektirmez; üretimde IdP/JWKS'e geçilir.
 * Her token benzersiz bir jti (JWT ID) taşır — logout sırasında tüm token'ı saklamadan
 * yalnızca jti'yi kara listeye almak için kullanılır (bkz. {@link TokenBlacklist}).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${payguard.security.jwt-secret}") String secret,
                      @Value("${payguard.security.jwt-expiration-ms:3600000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String issue(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Token geçerliyse (imza + süre) claim'lerini döner; geçersizse null. */
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
