package com.fraud.infrastructure.anomaly;

import com.fraud.application.anomaly.AnomalyDetector;
import com.fraud.application.anomaly.AnomalyResult;
import com.fraud.application.anomaly.FraudTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op detector activated when AI is disabled (fraud.ai.enabled=false).
 *
 * The anomaly check is skipped (every transaction is treated as clean). This guarantees the
 * handler always finds an AnomalyDetector bean; the flow never breaks.
 */
@Component
@ConditionalOnProperty(name = "fraud.ai.enabled", havingValue = "false")
public class NoOpAnomalyDetector implements AnomalyDetector {

    @Override
    public AnomalyResult check(FraudTransaction transaction) {
        return new AnomalyResult(false, 0.0, "AI disabled");
    }
}
