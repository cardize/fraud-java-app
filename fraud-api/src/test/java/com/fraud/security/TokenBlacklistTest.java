package com.fraud.security;

import com.fraud.api.security.TokenBlacklist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistTest {

    private final TokenBlacklist blacklist = new TokenBlacklist(3_600_000);

    @Test
    void revokedJtiIsReportedAsRevoked() {
        blacklist.revoke("jti-1");

        assertTrue(blacklist.isRevoked("jti-1"));
    }

    @Test
    void unknownJtiIsNotRevoked() {
        assertFalse(blacklist.isRevoked("never-seen-jti"));
    }

    @Test
    void nullJtiIsSafeAndNeverRevoked() {
        blacklist.revoke(null); // must not throw

        assertFalse(blacklist.isRevoked(null));
    }
}
