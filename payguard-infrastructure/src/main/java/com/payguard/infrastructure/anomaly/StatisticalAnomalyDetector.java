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
 * Hybrid statistical + rule-based anomaly detection.
 * Hybrid score: finalScore = (zScore + frequency + time + model) / 4.
 *
 * NOTE: The real ML model is represented here by pure statistics. To plug in an actual model
 * (e.g. DJL/ONNX), it's enough to replace modelScore with the model's output — the interface/flow
 * stays the same.
 *
 * Active when: payguard.ai.enabled=true (default). If false, {@link NoOpAnomalyDetector} takes over.
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

        // Be cautious while the model hasn't "learned" yet (not enough history)
        if (stats.count() < 5) {
            stats.update(tx.amount(), tx.transactionDate());
            return new AnomalyResult(true, 1.0, "Insufficient history — treated as suspicious as a precaution");
        }

        int hourOfDay = tx.transactionDate().atZone(ZoneOffset.UTC).getHour();
        double mean = stats.mean();
        double std = stats.stdDev();

        double zScore = std > 0 ? Math.min(1.0, Math.abs(tx.amount() - mean) / (3 * std)) : 0.0;
        double frequencyScore = Math.min(1.0, stats.frequencyLastHour(tx.transactionDate()) / 10.0);
        double timeScore = (hourOfDay >= 0 && hourOfDay < 6) ? 1.0 : 0.0;
        double modelScore = 0.0; // <-- DJL/ONNX model output goes here

        double finalScore = (zScore + frequencyScore + timeScore + modelScore) / 4.0;

        // Additional heuristic anomaly rules
        boolean anomaly = finalScore > SCORE_THRESHOLD
                || (mean > 0 && tx.amount() > mean * 3)
                || (hourOfDay < 6 && tx.amount() > mean * 2)
                || stats.frequencyLastHour(tx.transactionDate()) > 10;

        String reason = String.format(
                "z=%.2f freq=%.2f time=%.2f -> score=%.2f (mean=%.1f, std=%.1f)",
                zScore, frequencyScore, timeScore, finalScore, mean, std);

        stats.update(tx.amount(), tx.transactionDate());
        log.info("Anomaly check [{}]: anomaly={} {}", tx.shadowCardNo(), anomaly, reason);

        return new AnomalyResult(anomaly, finalScore, reason);
    }
}
