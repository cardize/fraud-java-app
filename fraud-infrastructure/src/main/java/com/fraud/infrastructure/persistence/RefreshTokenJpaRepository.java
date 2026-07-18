package com.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * ATOMIC one-shot consume: flips used=false -> true in a single conditional UPDATE.
     * Returns 0 when the token was ALREADY used — including the race where two concurrent
     * /refresh calls carry the same token: under READ_COMMITTED both could read used=false, but
     * only one of these updates can win; the loser's 0 is the reuse signal. This closes the
     * read-check-write gap the entity-level markUsed() approach had (external review finding C).
     */
    @Modifying
    @Query("update RefreshToken t set t.used = true where t.tokenHash = :hash and t.used = false")
    int markUsedIfUnused(@Param("hash") String hash);

    /** Revokes ALL of a user's refresh tokens (logout, or rotation-reuse theft response). */
    @Modifying
    @Query("delete from RefreshToken t where t.username = :username")
    int deleteByUsername(@Param("username") String username);

    /** Retention: expired tokens (used or not) are dead weight — cleaned daily by DataRetentionJob. */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
