package com.fraud.application.scenarios;

/**
 * PORT: write-time validation of rule expressions.
 *
 * The runtime evaluator already runs expressions in a locked-down context (see RuleEvaluator in
 * infrastructure) — but that alone would let a broken or unsafe expression sit in the DB and fail
 * on the hot path, per transaction. Validating at WRITE time is defense-in-depth: the admin gets
 * immediate feedback and only evaluable, boolean, side-effect-free expressions ever reach the DB.
 */
public interface ExpressionValidator {

    /**
     * @throws IllegalArgumentException when the expression has a syntax error, uses anything
     *         beyond property reads/operators (method/type/constructor calls), references unknown
     *         properties, or does not evaluate to a boolean
     */
    void validate(String expression);
}
