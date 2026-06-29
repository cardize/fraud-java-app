package com.payguard.application.queue;

import java.util.UUID;

/**
 * Kuyruğa atılan offline iş mesajı.
 *
 * .NET karşılığı: PayGuard.Application/Mq/Requests/ProcessOfflineScenariosRequest.
 */
public record OfflineOperation(
        UUID transactionId,
        int module,
        String fraudResponseCode,
        String tenant
) {
}
