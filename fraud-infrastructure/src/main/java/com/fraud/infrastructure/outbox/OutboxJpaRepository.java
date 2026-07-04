package com.fraud.infrastructure.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for outbox messages.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Fetches a bounded number of pending messages (oldest first), LOCKING them.
     * PESSIMISTIC_WRITE + SKIP LOCKED (timeout -2): in a multi-instance deployment, two relays
     * never grab the same row — locked rows are skipped, preventing double-processing.
     * (Translates to FOR UPDATE SKIP LOCKED on Postgres.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);

    /**
     * Deletes PROCESSED records older than the given cutoff.
     * Without this, the outbox table would grow unbounded and query/index performance would
     * degrade over time.
     */
    @Modifying
    @Query("delete from OutboxMessage m where m.status = com.fraud.infrastructure.outbox.OutboxStatus.PROCESSED " +
           "and m.processedAt < :cutoff")
    int deleteProcessedBefore(Instant cutoff);

    /** Backlog size for the fraud.outbox.pending gauge (cheap: covered by the status+created_at index). */
    long countByStatus(OutboxStatus status);
}
