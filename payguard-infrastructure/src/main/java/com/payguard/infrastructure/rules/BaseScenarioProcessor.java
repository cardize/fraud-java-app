package com.payguard.infrastructure.rules;

import com.payguard.application.fraud.FraudParameters;
import com.payguard.application.fraud.ScenarioProcessor;
import com.payguard.domain.rule.Rule;
import com.payguard.domain.rule.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Tüm ürün tipleri için ORTAK senaryo yürütme mantığı (paralel değerlendirme + öncelik kararı).
 *
 * .NET karşılığı: PayGRulesEngine/Processors/Implementations/BaseScenarioProcessor.cs (abstract).
 * Alt sınıflar yalnızca desteklediği ürün tipini bildirir; tüm motor mantığı burada tek yerde.
 *
 * Karar: tüm senaryolar değerlendirilir; "hit" olanlardan EN YÜKSEK ÖNCELİKLİ (priority en küçük)
 * olanın fraudResponseCode'u döner. Hiç hit yoksa "APPROVE".
 */
public abstract class BaseScenarioProcessor implements ScenarioProcessor {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APPROVE = "APPROVE";

    private final ScenarioCatalog scenarioCatalog;
    private final RuleEvaluator ruleEvaluator;
    private final boolean parallel;
    private final int maxParallelism;

    protected BaseScenarioProcessor(ScenarioCatalog scenarioCatalog,
                                    RuleEvaluator ruleEvaluator,
                                    boolean parallel,
                                    int maxParallelism) {
        this.scenarioCatalog = scenarioCatalog;
        this.ruleEvaluator = ruleEvaluator;
        this.parallel = parallel;
        this.maxParallelism = maxParallelism;
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
        // .NET ConcurrentScenarioLimit / MaxDegreeOfParallelism karşılığı: sınırlı thread havuzu.
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, maxParallelism));
        try {
            List<Future<Optional<Scenario>>> futures = scenarios.stream()
                    .map(scenario -> pool.submit(
                            () -> isHit(scenario, params) ? Optional.of(scenario) : Optional.<Scenario>empty()))
                    .toList();

            return futures.stream()
                    .map(this::get)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } finally {
            pool.shutdown();
        }
    }

    private Optional<Scenario> get(Future<Optional<Scenario>> f) {
        try {
            return f.get();
        } catch (Exception e) {
            log.error("Senaryo değerlendirme hatası", e);
            return Optional.empty();
        }
    }

    /** Senaryonun TÜM kuralları true ise hit. */
    private boolean isHit(Scenario scenario, FraudParameters params) {
        for (Rule rule : scenario.rules()) {
            if (!ruleEvaluator.evaluate(rule, params)) {
                return false;
            }
        }
        log.info("HIT [{}] senaryo: {} -> {}", supportedType(), scenario.name(), scenario.fraudResponseCode());
        return true;
    }
}
