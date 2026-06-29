package com.payguard.application.fraud;

import com.payguard.domain.shared.ProductType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Online senaryo yürütmesinin uygulama-katmanı girişi + ürün tipine göre yönlendirme.
 *
 * .NET karşılığı: ScenarioService.ProcessOnlineScenarios + ScenarioProcessorFactory.
 * Spring tüm ScenarioProcessor implementasyonlarını (CARD, PF, ...) bir LİSTE olarak enjekte eder;
 * biz de ürün tipi -> processor eşlemesini kurarız (factory deseni). Yeni ürün eklemek =
 * yeni bir ScenarioProcessor bean'i yazmak; burada hiçbir değişiklik gerekmez.
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
