package com.payguard.infrastructure.persistence;

import com.payguard.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
