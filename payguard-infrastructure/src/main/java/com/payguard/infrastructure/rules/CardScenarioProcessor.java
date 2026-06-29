package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kart (CARD) senaryo işlemcisi.
 *
 * .NET karşılığı: PayGRulesEngine/Processors/Implementations/Card/CardScenarioProcessor.cs
 * Tüm motor mantığı {@link BaseScenarioProcessor}'da; burada yalnızca ürün tipi belirtilir.
 */
@Component
public class CardScenarioProcessor extends BaseScenarioProcessor {

    public CardScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                 RuleEvaluator ruleEvaluator,
                                 @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                 @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        super(scenarioCatalog, ruleEvaluator, parallel, maxParallelism);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.CARD;
    }
}
