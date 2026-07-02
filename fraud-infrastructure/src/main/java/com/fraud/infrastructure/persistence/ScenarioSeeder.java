package com.fraud.infrastructure.persistence;

import com.fraud.domain.rule.RuleType;
import com.fraud.domain.shared.ProductType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds sample scenarios into the DB at startup (if the table is empty).
 *
 * The in-memory DB starts empty every boot, so we seed it to make the flow testable end-to-end.
 * This class would be removed in production; scenarios are entered via a management UI instead.
 */
@Component
public class ScenarioSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSeeder.class);

    private final ScenarioJpaRepository repository;

    public ScenarioSeeder(ScenarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (repository.existsByProductType(ProductType.CARD)) {
            return; // already seeded
        }

        // CARD scenarios
        ScenarioRow highAmount = new ScenarioRow(
                "High Amount", ProductType.CARD, 1, 1, "REJECT",
                List.of(new RuleRow("Amount exceeds threshold", RuleType.SIMPLE, "amountValue > threshold")));

        ScenarioRow nightLargeSpend = new ScenarioRow(
                "Large Nighttime Spend", ProductType.CARD, 1, 2, "REVIEW",
                List.of(
                        new RuleRow("Nighttime hour", RuleType.SIMPLE, "hourOfDay >= 0 and hourOfDay < 6"),
                        new RuleRow("Amount > 1000", RuleType.SIMPLE, "amountValue > 1000")));

        // A sample scenario for PF (to show the new product type also works from the DB)
        ScenarioRow pfHighAmount = new ScenarioRow(
                "PF High Amount", ProductType.PF, 1, 1, "REJECT",
                List.of(new RuleRow("PF amount > 10000", RuleType.SIMPLE, "amountValue > 10000")));

        repository.saveAll(List.of(highAmount, nightLargeSpend, pfHighAmount));
        log.info("Sample scenarios loaded: {} total", repository.count());
    }
}
