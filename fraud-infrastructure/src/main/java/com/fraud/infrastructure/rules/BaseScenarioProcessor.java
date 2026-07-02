package com.fraud.infrastructure.rules;

import com.fraud.application.fraud.FraudParameters;
import com.fraud.application.fraud.ScenarioProcessor;
import com.fraud.domain.rule.Rule;
import com.fraud.domain.rule.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * SHARED scenario execution logic for all product types (parallel evaluation + priority decision).
 *
 * Subclasses only declare which product type they support; the entire engine logic lives here in
 * one place.
 *
 * Decision: all scenarios are evaluated; among the ones that "hit", the HIGHEST-PRIORITY one
 * (lowest priority number) wins its fraudResponseCode. "APPROVE" if none hit.
 */
public abstract class BaseScenarioProcessor implements ScenarioProcessor {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APPROVE = "APPROVE";

    private final ScenarioCatalog scenarioCatalog;
    private final RuleEvaluator ruleEvaluator;
    private final boolean parallel;
    private final ExecutorService executor;

    protected BaseScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                    RuleEvaluator ruleEvaluator,
                                    boolean parallel,
                                    ExecutorService executor) {
        this.scenarioCatalog = scenarioCatalog;
        this.ruleEvaluator = ruleEvaluator;
        this.parallel = parallel;
        this.executor = executor;
    }

    @Override
    public final String processOnlineScenarios(int module, FraudParameters params) {
        List<Scenario> scenarios = scenarioCatalog.loadOnlineScenarios(supportedType(), module);

        List<Scenario> hits = parallel
                ? evaluateParallel(scenarios, params)
                : scenarios.stream().filter(s -> isHit(s, params)).toList();

        return hits.stream()
                .min(Comparator.comparingInt(Scenario::priority))
                .map(Scenario::fraudResponseCode)
                .orElse(APPROVE);
    }

    private List<Scenario> evaluateParallel(List<Scenario> scenarios, FraudParameters params) {
        // Work is submitted to the shared, bounded pool (no new pool is opened per request).
        List<Future<Optional<Scenario>>> futures = scenarios.stream()
                .map(scenario -> executor.submit(
                        () -> isHit(scenario, params) ? Optional.of(scenario) : Optional.<Scenario>empty()))
                .toList();

        return futures.stream()
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<Scenario> get(Future<Optional<Scenario>> f) {
        try {
            return f.get();
        } catch (Exception e) {
            log.error("Scenario evaluation error", e);
            return Optional.empty();
        }
    }

    /** A scenario hits only if ALL of its rules are true. */
    private boolean isHit(Scenario scenario, FraudParameters params) {
        for (Rule rule : scenario.rules()) {
            if (!ruleEvaluator.evaluate(rule, params)) {
                return false;
            }
        }
        log.info("HIT [{}] scenario: {} -> {}", supportedType(), scenario.name(), scenario.fraudResponseCode());
        return true;
    }
}
