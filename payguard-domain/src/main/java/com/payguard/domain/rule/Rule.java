package com.payguard.domain.rule;

import java.io.Serializable;

/**
 * A single rule. Holds a SpEL (Spring Expression Language) expression for evaluation.
 *
 * @param id            rule identifier
 * @param name          human-readable name
 * @param type          rule type (selects the evaluator strategy)
 * @param expression    SpEL expression; the rule "hits" when it evaluates to true against
 *                      FraudParameters, e.g. "amount > threshold" or "hourOfDay < 6 and amount > 5000"
 */
public record Rule(
        long id,
        String name,
        RuleType type,
        String expression
) implements Serializable {
}
