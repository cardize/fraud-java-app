package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * TrKart scenario processor.
 */
@Component
public class TrKartScenarioProcessor extends BaseScenarioProcessor {

    public TrKartScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                   RuleEvaluator ruleEvaluator,
                                   @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                   @Qualifier("scenarioExecutor") ExecutorService executor) {
        super(scenarioCatalog, ruleEvaluator, parallel, executor);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.TRKART;
    }
}
