package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PayCell senaryo işlemcisi.
 *
 * .NET karşılığı: PayCell ürün-tipi processor'ı.
 */
@Component
public class PayCellScenarioProcessor extends BaseScenarioProcessor {

    public PayCellScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                    RuleEvaluator ruleEvaluator,
                                    @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                    @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        super(scenarioCatalog, ruleEvaluator, parallel, maxParallelism);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PAYCELL;
    }
}
