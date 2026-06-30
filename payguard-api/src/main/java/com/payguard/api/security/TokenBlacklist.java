package com.payguard.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * İptal edilmiş (logout edilmiş) token'ların jti'lerini (JWT ID) tutar.
 *
 * Stateless JWT doğası gereği "logout" kavramına sahip değildir — token, süresi dolana kadar
 * her zaman geçerlidir. Bu kara liste, kullanıcı açıkça çıkış yaptığında token'ı erken
 * geçersiz kılmayı sağlar. Tüm token DEĞİL, yalnızca jti saklanır (ufak bellek ayak izi);
 * giriş, en uzun olası token ömrü kadar TTL'lidir — bunu aşan kayıt zaten gereksizdir.
 */
@Component
public class TokenBlacklist {

    private final Cache<String, Boolean> revoked;

    public TokenBlacklist(@Value("${payguard.security.jwt-expiration-ms:3600000}") long jwtExpirationMs) {
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
