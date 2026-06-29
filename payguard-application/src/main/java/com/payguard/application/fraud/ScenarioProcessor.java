package com.payguard.application.fraud;

import com.payguard.domain.shared.ProductType;

/**
 * Senaryo işleme PORT'u. Her ürün tipi (CARD, PF, PayCell...) için bir implementasyon olur.
 *
 * Port APPLICATION'da; implementasyonlar (SpEL/DB kullanan) INFRASTRUCTURE'dadır.
 * Böylece application, kural motorunun teknik detayını bilmez.
 *
 * .NET karşılığı: PayGRulesEngine/Processors/Interfaces/IScenarioProcessor.
 */
public interface ScenarioProcessor {

    /** Bu işlemcinin desteklediği ürün tipi (factory eşlemesi için). */
    ProductType supportedType();

    /** Online senaryoları çalıştırır ve fraud yanıt kodunu döner. */
    String processOnlineScenarios(int module, FraudParameters params);
}
