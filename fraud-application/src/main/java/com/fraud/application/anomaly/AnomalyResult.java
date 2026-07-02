package com.fraud.application.anomaly;

/**
 * Anomaly check result.
 *
 * @param anomaly whether the transaction is anomalous (suspicious)
 * @param score   hybrid anomaly score (0..1+ range)
 * @param reason  human-readable explanation of the decision
 */
public record AnomalyResult(
        boolean anomaly,
        double score,
        String reason
) {
}
