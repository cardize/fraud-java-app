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
    void consumeReturnsUsernameWhenTheAtomicConsumeWins() {
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        // RACE FIX (review finding C): consuming is a single conditional UPDATE, not an
        // entity-level read-check-write; 1 affected row = this call won the one-shot consume.
        when(repository.markUsedIfUnused(anyString())).thenReturn(1);

        Optional<String> result = service.consume("raw-token");

        assertEquals(Optional.of("bob"), result);
        verify(repository, never()).deleteByUsername(anyString());
    }

    @Test
    void consumeReturnsEmptyForAnExpiredButNeverUsedToken() {
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().minusSeconds(10));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertTrue(service.consume("raw-token").isEmpty());
        // Plain expiry is NOT a theft signal (unlike reuse) -> no family-wide revocation,
        // and the atomic consume is never even attempted.
        verify(repository, never()).markUsedIfUnused(anyString());
        verify(repository, never()).deleteByUsername(anyString());
    }

    @Test
    void losingTheAtomicConsumeIsTreatedAsTheftAndRevokesTheWholeFamily() {
        // Covers BOTH reuse of a long-consumed token and the concurrent double-spend race: in
        // either case the conditional UPDATE affects 0 rows because used was already true.
        RefreshToken token = new RefreshToken("irrelevant-hash", "bob", Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(repository.markUsedIfUnused(anyString())).thenReturn(0);

        assertTrue(service.consume("raw-token").isEmpty());
        verify(repository).deleteByUsername(eq("bob"));
    }

    @Test
    void revokeAllDelegatesToTheRepository() {
        service.revokeAll("carol");

        verify(repository).deleteByUsername("carol");
    }
}
