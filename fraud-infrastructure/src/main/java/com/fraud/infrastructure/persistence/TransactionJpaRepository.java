package com.fraud.infrastructure.persistence;

import com.fraud.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository (infrastructure detail).
 *
 * Spring generates this interface's implementation AUTOMATICALLY. Because this type depends on
 * Spring Data, it lives in INFRASTRUCTURE, not APPLICATION; it is exposed outward through the
 * TransactionStore port (see JpaTransactionStore).
 */
public interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {

    @Modifying
    @Query("update Transaction t set t.latestRequest = false " +
           "where t.messageId = :messageId and t.module = :module and t.id <> :currentId")
    void markPreviousAsNotLatest(@Param("messageId") long messageId,
                                 @Param("module") int module,
                                 @Param("currentId") UUID currentId);

    /**
     * Bulk-deletes transactions older than the cutoff (retention, see DataRetentionJob).
     * Runs as a single DELETE in the DB — no entities are loaded; backed by the
     * transaction_date index (V5).
     */
    @Modifying
    @Query("delete from Transaction t where t.transactionDate < :cutoff")
    int deleteByTransactionDateBefore(@Param("cutoff") Instant cutoff);
}
