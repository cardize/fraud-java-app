package com.fraud.application.queue;

import java.util.UUID;

/**
 * Offline work message dispatched to the queue/outbox.
 */
public record OfflineOperation(
        UUID transactionId,
        int module,
        String fraudResponseCode,
        String tenant
) {
}
