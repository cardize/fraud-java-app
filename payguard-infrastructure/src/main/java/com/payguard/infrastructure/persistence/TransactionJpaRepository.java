package com.payguard.infrastructure.persistence;

import com.payguard.domain.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Spring Data JPA repository (altyapı detayı).
 *
 * .NET karşılığı: EF Core DbSet + LINQ / Dapper sorguları.
 * Spring, bu arayüzün implementasyonunu OTOMATİK üretir. Bu tip Spring Data'ya
 * bağımlı olduğu için APPLICATION'da değil, INFRASTRUCTURE'da durur; dışarıya
 * TransactionStore portu üzerinden açılır (bkz. JpaTransactionStore).
 */
public interface TransactionJpaRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByMessageIdAndModule(long messageId, int module);

    @Modifying
    @Query("update Transaction t set t.latestRequest = false " +
           "where t.messageId = :messageId and t.module = :module and t.id <> :currentId")
    void markPreviousAsNotLatest(@Param("messageId") long messageId,
                                 @Param("module") int module,
                                 @Param("currentId") UUID currentId);
}
