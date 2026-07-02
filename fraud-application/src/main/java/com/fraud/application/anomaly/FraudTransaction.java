package com.fraud.application.anomaly;

import java.time.Instant;
import java.util.UUID;

/**
 * Transaction summary submitted for anomaly checking.
 */
public record FraudTransaction(
        UUID transactionId,
        String shadowCardNo,
        double amount,
        String merchantId,
        Instant transactionDate
) {
}
