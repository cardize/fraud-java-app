package com.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revokes ALL of a user's refresh tokens (logout, or rotation-reuse theft response). */
    @Modifying
    @Query("delete from RefreshToken t where t.username = :username")
    int deleteByUsername(@Param("username") String username);

    /** Retention: expired tokens (used or not) are dead weight — cleaned daily by DataRetentionJob. */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);
}
