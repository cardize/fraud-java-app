package com.payguard.infrastructure.anomaly;

import com.payguard.application.anomaly.AnomalyDetector;
import com.payguard.application.anomaly.AnomalyResult;
import com.payguard.application.anomaly.FraudTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI kapalıyken (payguard.ai.enabled=false) devreye giren no-op detektör.
 *
 * Anomali kontrolü atlanır (her işlem temiz sayılır). Böylece handler her zaman bir
 * AnomalyDetector bean'i bulur; akış kırılmaz.
 */
@Component
@ConditionalOnProperty(name = "payguard.ai.enabled", havingValue = "false")
public class NoOpAnomalyDetector implements AnomalyDetector {

    @Override
    public AnomalyResult check(FraudTransaction transaction) {
        return new AnomalyResult(false, 0.0, "AI devre dışı");
    }
}
