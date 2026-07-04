package com.fraud.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MessageClaimJpaRepository extends JpaRepository<MessageClaim, Long> {

    /**
     * Bulk-deletes claims older than the cutoff (retention). Single DELETE statement, backed by
     * the claimed_at index (V5). NOTE: deleting a claim re-opens duplicate detection for that
     * message id — the retention period must exceed any realistic upstream retry window.
     */
    @Modifying
    @Query("delete from MessageClaim c where c.claimedAt < :cutoff")
    int deleteByClaimedAtBefore(@Param("cutoff") Instant cutoff);
}
