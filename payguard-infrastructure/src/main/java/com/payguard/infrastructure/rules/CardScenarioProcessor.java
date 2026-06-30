package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Kart (CARD) senaryo işlemcisi.
 * Tüm motor mantığı {@link BaseScenarioProcessor}'da; burada yalnızca ürün tipi belirtilir.
 */
@Component
public class CardScenarioProcessor extends BaseScenarioProcessor {

    public CardScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                 RuleEvaluator ruleEvaluator,
                                 @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                 @Qualifier("scenarioExecutor") ExecutorService executor) {
        super(scenarioCatalog, ruleEvaluator, parallel, executor);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.CARD;
    }
}
