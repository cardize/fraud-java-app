package com.fraud.security;

import com.fraud.api.security.RefreshTokenService;
import com.fraud.infrastructure.persistence.RefreshToken;
import com.fraud.infrastructure.persistence.RefreshTokenJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The repository is mocked (no DB needed) — these tests lock in RefreshTokenService's branching
 * logic: unknown / expired / already-used / valid. RefreshToken has no getters for tokenHash or
 * expiresAt, so assertions go through its public API only (isExpired(), isUsed(), getUsername()),
 * same as production code would.
 */
class RefreshTokenServiceTest {

    private static final long EXPIRATION_MS = 604_800_000L; // 7 days, same as the production default

    private final RefreshTokenJpaRepository repository = mock(RefreshTokenJpaRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, EXPIRATION_MS);

    @Test
    void issueReturnsA256BitRawTokenAndSavesItsHashWithTheRightExpiry() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        String raw = service.issue("alice");

        assertEquals(32, Base64.getUrlDecoder().decode(raw).length, "the raw token must be 256 bits (32 bytes)");
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals("alice", saved.getUsername());
        assertFalse(saved.isUsed());
        // Verify the expiry timing indirectly (no getter exists): not yet expired just before the
        // window closes, expired just after.
        assertFalse(saved.isExpired(Instant.now().plusMillis(EXPIRATION_MS).minusSeconds(5)));
        assertTrue(saved.isExpired(Instant.now().plusMillis(EXPIRATION_MS).plusSeconds(5)));
    }

    @Test
    void consumeReturnsEmptyForAnUnknownToken() {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertTrue(service.consume("unknown-raw-token").isEmpty());
        verify(repository, never()).deleteByUsername(anyString());
    }

    @Test
    void consumeReturnsUsernameAndMarksTheTokenUsedForAValidToken() {
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        Optional<String> result = service.consume("raw-token");

        assertEquals(Optional.of("bob"), result);
        assertTrue(token.isUsed(), "the SAME entity instance must be marked used (one-shot rotation)");
    }

    @Test
    void consumeReturnsEmptyForAnExpiredButNeverUsedToken() {
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().minusSeconds(10));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertTrue(service.consume("raw-token").isEmpty());
        // Plain expiry is NOT a theft signal (unlike reuse) -> no family-wide revocation.
        verify(repository, never()).deleteByUsername(anyString());
    }

    @Test
    void consumeOfAnAlreadyUsedTokenIsTreatedAsTheftAndRevokesTheWholeFamily() {
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().plusSeconds(60));
        token.markUsed();
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertTrue(service.consume("raw-token").isEmpty());
        verify(repository).deleteByUsername(eq("bob"));
    }

    @Test
    void revokeAllDelegatesToTheRepository() {
        service.revokeAll("carol");

        verify(repository).deleteByUsername("carol");
    }
}
