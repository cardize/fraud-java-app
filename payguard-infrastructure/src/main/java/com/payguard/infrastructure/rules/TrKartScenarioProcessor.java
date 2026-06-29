package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TrKart senaryo işlemcisi.
 *
 * .NET karşılığı: TrKart ürün-tipi processor'ı.
 */
@Component
public class TrKartScenarioProcessor extends BaseScenarioProcessor {

    public TrKartScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                   RuleEvaluator ruleEvaluator,
                                   @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                   @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        super(scenarioCatalog, ruleEvaluator, parallel, maxParallelism);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.TRKART;
    }
}
