package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Payment Facilitator (PF) senaryo işlemcisi.
 * Yeni ürün eklemenin ne kadar kolay olduğunu gösterir: sadece bu bean + supportedType().
 */
@Component
public class PfScenarioProcessor extends BaseScenarioProcessor {

    public PfScenarioProcessor(ScenarioCatalog scenarioCatalog,
                               RuleEvaluator ruleEvaluator,
                               @Value("${payguard.scenario.parallel:true}") boolean parallel,
                               @Qualifier("scenarioExecutor") ExecutorService executor) {
        super(scenarioCatalog, ruleEvaluator, parallel, executor);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PF;
    }
}
