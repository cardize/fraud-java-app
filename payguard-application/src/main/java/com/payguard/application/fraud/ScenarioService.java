package com.payguard.application.fraud;

import com.payguard.domain.shared.ProductType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Online senaryo yürütmesinin uygulama-katmanı girişi + ürün tipine göre yönlendirme (factory).
 *
 * Spring tüm ScenarioProcessor implementasyonlarını (CARD, PF, ...) bir LİSTE olarak enjekte eder;
 * ürün tipi -> processor eşlemesi kurulur. Yeni ürün eklemek = yeni bir ScenarioProcessor bean'i
 * yazmak; burada hiçbir değişiklik gerekmez (Open/Closed).
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
            throw new UnsupportedOperationException("Ürün tipi için işlemci yok: " + productType);
        }
        return processor.processOnlineScenarios(module, params);
    }
}
