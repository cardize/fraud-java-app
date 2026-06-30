package com.payguard.application.anomaly;

import java.time.Instant;
import java.util.UUID;

/**
 * Anomali kontrolüne giren işlem özeti.
 */
public record FraudTransaction(
        UUID transactionId,
        String shadowCardNo,
        double amount,
        String merchantId,
        Instant transactionDate
) {
}
