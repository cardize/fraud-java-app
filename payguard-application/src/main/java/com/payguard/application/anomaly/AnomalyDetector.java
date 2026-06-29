package com.payguard.application.anomaly;

/**
 * Anomali tespiti PORT'u.
 *
 * Application yalnızca bu arayüzü bilir; gerçek implementasyon (istatistiksel skorlama bugün,
 * ileride DJL/ONNX modeli) INFRASTRUCTURE'dadır. Böylece ML kütüphanesini değiştirmek
 * application/domain'i etkilemez.
 *
 * .NET karşılığı: PayGuard.Application.AI/Services/Interfaces/IAnomalyDetectionService.
 */
public interface AnomalyDetector {
    AnomalyResult check(FraudTransaction transaction);
}
