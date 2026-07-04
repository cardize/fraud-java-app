package com.fraud.application.fraud;

import com.fraud.domain.shared.ProductType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Application-layer entry point for running online scenarios + routing by product type (factory).
 *
 * Spring injects every ScenarioProcessor implementation (CARD, PF, ...) as a LIST; the product
 * type -> processor mapping is built from that. Adding a new product = writing a new
 * ScenarioProcessor bean; nothing here needs to change (Open/Closed).
 */
@Service
public class ScenarioService {

    private final Map<ProductType, ScenarioProcessor> processors = new EnumMap<>(ProductType.class);
    private final MeterRegistry meterRegistry;

    public ScenarioService(List<ScenarioProcessor> processorBeans, MeterRegistry meterRegistry) {
        for (ScenarioProcessor p : processorBeans) {
            processors.put(p.supportedType(), p);
        }
        this.meterRegistry = meterRegistry;
    }

    public String processOnlineScenarios(ProductType productType, int module, FraudParameters params) {
        ScenarioProcessor processor = processors.get(productType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor for product type: " + productType);
        }
        // The engine's latency is THE business-critical latency (it runs synchronously inside the
        // client's request) — timed per product type: fraud.scenario.duration{productType=...}.
        return meterRegistry.timer("fraud.scenario.duration", "productType", productType.name())
                .record(() -> processor.processOnlineScenarios(module, params));
    }
}
