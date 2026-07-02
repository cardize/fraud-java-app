package com.fraud.application.anomaly;

/**
 * Anomaly detection PORT.
 *
 * Application only knows this interface; the real implementation (statistical scoring today,
 * a DJL/ONNX model tomorrow) lives in INFRASTRUCTURE. Swapping the ML library never affects
 * application/domain.
 */
public interface AnomalyDetector {
    AnomalyResult check(FraudTransaction transaction);
}
