package com.payguard.infrastructure.outbox;

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
 * Outbox mesajları için Spring Data JPA repository'si.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Yayımlanmayı bekleyen mesajları (en eski önce) sınırlı sayıda, KİLİTLEYEREK getirir.
     * PESSIMISTIC_WRITE + SKIP LOCKED (timeout -2): çoklu-instance'ta iki relay aynı satırı almaz,
     * kilitli satırlar atlanır → çift işleme önlenir. (Postgres'te FOR UPDATE SKIP LOCKED'a çevrilir.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);

    /**
     * Belirtilen tarihten eski, işlenmiş (PROCESSED) kayıtları siler.
     * Bu olmadan outbox tablosu sınırsız büyür ve sorgu/index performansı zamanla bozulur.
     */
    @Modifying
    @Query("delete from OutboxMessage m where m.status = com.payguard.infrastructure.outbox.OutboxStatus.PROCESSED " +
           "and m.processedAt < :cutoff")
    int deleteProcessedBefore(Instant cutoff);
}
