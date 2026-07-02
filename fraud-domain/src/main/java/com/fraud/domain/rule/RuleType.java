package com.fraud.domain.rule;

/**
 * Rule type — each type may have its own evaluator (executor) strategy.
 * Only SIMPLE is used today; the Strategy pattern is ready for the others.
 */
public enum RuleType {
    SIMPLE,
    COMPLEX,
    LINKED,
    PERIODIC,
    RULE_SET
}
