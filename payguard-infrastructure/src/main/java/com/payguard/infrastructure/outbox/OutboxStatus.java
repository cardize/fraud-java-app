package com.payguard.infrastructure.outbox;

/**
 * Outbox mesaj durumu.
 * PENDING: yazıldı, henüz yayımlanmadı. PROCESSED: relay tarafından yayımlandı.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSED
}
