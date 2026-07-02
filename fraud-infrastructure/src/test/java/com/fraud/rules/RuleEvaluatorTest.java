package com.fraud.rules;

import com.fraud.application.fraud.FraudParameters;
import com.fraud.domain.rule.Rule;
import com.fraud.domain.rule.RuleType;
import com.fraud.infrastructure.rules.RuleEvaluator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of SpEL-based rule evaluation.
 */
class RuleEvaluatorTest {

    private final RuleEvaluator evaluator = new RuleEvaluator();

    private FraudParameters params(double amount, int hour) {
        Instant date = LocalDateTime.of(2026, 1, 1, hour, 0).toInstant(ZoneOffset.UTC);
        return new FraudParameters(UUID.randomUUID(), "CARD123", BigDecimal.valueOf(amount),
                "MERCH1", date, 5000.0);
    }

    @Test
    void highAmountRuleHits() {
        Rule rule = new Rule(1, "high amount", RuleType.SIMPLE, "amountValue > threshold");
        assertTrue(evaluator.evaluate(rule, params(6000, 14)));
        assertFalse(evaluator.evaluate(rule, params(100, 14)));
    }

    @Test
    void nightHourRuleHits() {
        Rule rule = new Rule(2, "nighttime hour", RuleType.SIMPLE, "hourOfDay >= 0 and hourOfDay < 6");
        assertTrue(evaluator.evaluate(rule, params(100, 3)));
        assertFalse(evaluator.evaluate(rule, params(100, 15)));
    }
}
