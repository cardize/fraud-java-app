package com.fraud.application.fraud;

import com.fraud.domain.shared.ProductType;
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

    public ScenarioService(List<ScenarioProcessor> processorBeans) {
        for (ScenarioProcessor p : processorBeans) {
            processors.put(p.supportedType(), p);
        }
    }

    public String processOnlineScenarios(ProductType productType, int module, FraudParameters params) {
        ScenarioProcessor processor = processors.get(productType);
        if (processor == null) {
            throw new UnsupportedOperationException("No processor for product type: " + productType);
        }
        return processor.processOnlineScenarios(module, params);
    }
}
