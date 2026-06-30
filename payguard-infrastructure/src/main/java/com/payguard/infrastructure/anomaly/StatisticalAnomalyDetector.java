package com.payguard.infrastructure.anomaly;

import com.payguard.application.anomaly.AnomalyDetector;
import com.payguard.application.anomaly.AnomalyResult;
import com.payguard.application.anomaly.FraudTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * İstatistiksel + kural tabanlı hibrit anomali tespiti.
 * Hibrit skor: finalScore = (zScore + frekans + zaman + model) / 4.
 *
 * NOT: Gerçek ML modeli burada saf-istatistikle temsil edilir. Bir model (örn. DJL/ONNX) takmak için
 * modelScore'u model çıktısıyla değiştirmek yeterli — arayüz/akış aynı kalır.
 *
 * Aktif olma koşulu: payguard.ai.enabled=true (varsayılan). false ise {@link NoOpAnomalyDetector} devreye girer.
 */
@Component
@ConditionalOnProperty(name = "payguard.ai.enabled", havingValue = "true", matchIfMissing = true)
public class StatisticalAnomalyDetector implements AnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(StatisticalAnomalyDetector.class);
    private static final double SCORE_THRESHOLD = 0.6;

    private final CardStatisticsStore statisticsStore;

    public StatisticalAnomalyDetector(CardStatisticsStore statisticsStore) {
        this.statisticsStore = statisticsStore;
    }

    @Override
    public AnomalyResult check(FraudTransaction tx) {
        CardStatistics stats = statisticsStore.getOrCreate(tx.shadowCardNo());

        // Model henüz "öğrenmediyse" (yeterli geçmiş yok) ihtiyatlı davran
        if (stats.count() < 5) {
            stats.update(tx.amount(), tx.transactionDate());
            return new AnomalyResult(true, 1.0, "Yetersiz geçmiş — ihtiyaten şüpheli");
        }

        int hourOfDay = tx.transactionDate().atZone(ZoneOffset.UTC).getHour();
        double mean = stats.mean();
        double std = stats.stdDev();

        double zScore = std > 0 ? Math.min(1.0, Math.abs(tx.amount() - mean) / (3 * std)) : 0.0;
        double frequencyScore = Math.min(1.0, stats.frequencyLastHour(tx.transactionDate()) / 10.0);
        double timeScore = (hourOfDay >= 0 && hourOfDay < 6) ? 1.0 : 0.0;
        double modelScore = 0.0; // <-- DJL/ONNX model çıktısı buraya

        double finalScore = (zScore + frequencyScore + timeScore + modelScore) / 4.0;

        // Ek sezgisel anomali kuralları
        boolean anomaly = finalScore > SCORE_THRESHOLD
                || (mean > 0 && tx.amount() > mean * 3)
                || (hourOfDay < 6 && tx.amount() > mean * 2)
                || stats.frequencyLastHour(tx.transactionDate()) > 10;

        String reason = String.format(
                "z=%.2f freq=%.2f time=%.2f -> score=%.2f (mean=%.1f, std=%.1f)",
                zScore, frequencyScore, timeScore, finalScore, mean, std);

        stats.update(tx.amount(), tx.transactionDate());
        log.info("Anomali kontrol [{}]: anomaly={} {}", tx.shadowCardNo(), anomaly, reason);

        return new AnomalyResult(anomaly, finalScore, reason);
    }
}
