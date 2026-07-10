package com.fraud.security;

import com.fraud.api.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JwtService is instantiated directly (its constructor takes plain values, no Spring context
 * needed) so these run as fast pure unit tests, covering edge cases the MockMvc integration tests
 * never exercise: expiry, a tampered/wrong-secret signature, and garbage input.
 */
class JwtServiceTest {

    // HS256 requires a key of at least 256 bits (32+ chars).
    private static final String SECRET = "unit-test-secret-key-at-least-32-characters-long";

    @Test
    void issueThenValidateRoundTripsSubjectAndRoles() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);

        String token = jwtService.issue("alice", List.of("ADMIN", "USER"));
        Claims claims = jwtService.validate(token);

        assertEquals("alice", claims.getSubject());
        assertEquals(Set.of("ADMIN", "USER"), Set.copyOf(claims.get("roles", List.class)));
    }

    @Test
    void eachIssuedTokenGetsAUniqueJti() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);

        String jti1 = jwtService.validate(jwtService.issue("alice", List.of("USER"))).getId();
        String jti2 = jwtService.validate(jwtService.issue("alice", List.of("USER"))).getId();

        assertNotEquals(jti1, jti2, "each token must be individually revocable via a unique jti");
    }

    @Test
    void expiredTokenFailsValidation() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1); // 1ms lifetime

        String token = jwtService.issue("alice", List.of("USER"));
        Thread.sleep(20); // let it actually expire

        assertNull(jwtService.validate(token));
    }

    @Test
    void tokenSignedWithADifferentSecretFailsValidation() {
        JwtService issuer = new JwtService(SECRET, 3_600_000);
        JwtService verifier = new JwtService("a-completely-different-secret-key-of-32+chars!!", 3_600_000);

        String token = issuer.issue("alice", List.of("USER"));

        assertNull(verifier.validate(token), "a token signed with a different key must never validate");
    }

    @Test
    void garbageInputFailsValidationWithoutThrowing() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000);

        assertNull(jwtService.validate("not-a-jwt-at-all"));
        assertNull(jwtService.validate(""));
    }
}
