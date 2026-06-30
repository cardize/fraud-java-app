package com.payguard.application.anomaly;

/**
 * Anomali kontrol sonucu.
 *
 * @param anomaly işlem anomali (şüpheli) mi
 * @param score   hibrit anomali skoru (0..1+ arası)
 * @param reason  kararın insan-okur açıklaması
 */
public record AnomalyResult(
        boolean anomaly,
        double score,
        String reason
) {
}
