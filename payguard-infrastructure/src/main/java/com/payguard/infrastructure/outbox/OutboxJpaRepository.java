package com.payguard.infrastructure.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Outbox mesajları için Spring Data JPA repository'si.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, Long> {

    /** Yayımlanmayı bekleyen mesajları (en eski önce) sınırlı sayıda getirir. */
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
