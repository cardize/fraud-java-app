package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Payment Facilitator (PF) senaryo işlemcisi.
 * Yeni ürün eklemenin ne kadar kolay olduğunu gösterir: sadece bu bean + supportedType().
 */
@Component
public class PfScenarioProcessor extends BaseScenarioProcessor {

    public PfScenarioProcessor(ScenarioCatalog scenarioCatalog,
                               RuleEvaluator ruleEvaluator,
                               @Value("${payguard.scenario.parallel:true}") boolean parallel,
                               @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        super(scenarioCatalog, ruleEvaluator, parallel, maxParallelism);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PF;
    }
}
