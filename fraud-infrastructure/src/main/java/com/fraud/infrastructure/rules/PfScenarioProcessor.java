package com.fraud.infrastructure.rules;

import com.fraud.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Payment Facilitator (PF) scenario processor.
 * Shows how easy it is to add a new product: just this bean + supportedType().
 */
@Component
public class PfScenarioProcessor extends BaseScenarioProcessor {

    public PfScenarioProcessor(ScenarioCatalog scenarioCatalog,
                               RuleEvaluator ruleEvaluator,
                               @Value("${fraud.scenario.parallel:true}") boolean parallel,
                               @Qualifier("scenarioExecutor") ExecutorService executor) {
        super(scenarioCatalog, ruleEvaluator, parallel, executor);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PF;
    }
}
