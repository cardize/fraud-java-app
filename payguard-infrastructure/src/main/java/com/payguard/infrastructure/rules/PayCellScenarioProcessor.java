package com.payguard.infrastructure.rules;

import com.payguard.domain.shared.ProductType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * PayCell scenario processor.
 */
@Component
public class PayCellScenarioProcessor extends BaseScenarioProcessor {

    public PayCellScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                    RuleEvaluator ruleEvaluator,
                                    @Value("${payguard.scenario.parallel:true}") boolean parallel,
                                    @Qualifier("scenarioExecutor") ExecutorService executor) {
        super(scenarioCatalog, ruleEvaluator, parallel, executor);
    }

    @Override
    public ProductType supportedType() {
        return ProductType.PAYCELL;
    }
}
