package com.payguard.application.queue;

import java.util.UUID;

/**
 * Kuyruğa/outbox'a atılan offline iş mesajı.
 */
public record OfflineOperation(
        UUID transactionId,
        int module,
        String fraudResponseCode,
        String tenant
) {
}
