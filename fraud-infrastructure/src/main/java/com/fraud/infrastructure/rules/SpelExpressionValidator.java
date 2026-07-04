package com.fraud.infrastructure.rules;

import com.fraud.application.fraud.FraudParameters;
import com.fraud.application.scenarios.ExpressionValidator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ADAPTER for the ExpressionValidator port: validates a rule expression exactly the way the
 * runtime evaluates it.
 *
 * Two stages, mirroring RuleEvaluator's runtime behavior:
 *  1) parse            — SpEL syntax errors are rejected;
 *  2) trial evaluation — against a fixed SAMPLE FraudParameters in the same locked-down
 *     SimpleEvaluationContext the hot path uses. This rejects method/type/constructor calls
 *     (e.g. an injection attempt like T(Runtime).exec(...)), unknown property names, and
 *     expressions that don't produce a boolean.
 *
 * Because validation and runtime share the same context rules, "saved successfully" implies
 * "will evaluate successfully" — no write-time/run-time drift.
 */
@Component
public class SpelExpressionValidator implements ExpressionValidator {

    /** Deterministic sample; only the SHAPE matters (which properties exist and their types). */
    private static final FraudParameters SAMPLE = new FraudParameters(
            new UUID(0L, 0L), "SAMPLE-CARD", BigDecimal.valueOf(100),
            "SAMPLE-MERCHANT", Instant.EPOCH, 5000.0);

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public void validate(String expression) {
        Expression parsed;
        try {
            parsed = parser.parseExpression(expression);
        } catch (ParseException e) {
            throw new IllegalArgumentException("SpEL syntax error: " + e.getSimpleMessage());
        }

        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(SAMPLE)
                .build();
        Boolean result;
        try {
            result = parsed.getValue(context, Boolean.class);
        } catch (EvaluationException e) {
            throw new IllegalArgumentException(
                    "not evaluable against fraud parameters (only property reads and operators are allowed): "
                            + e.getSimpleMessage());
        }
        if (result == null) {
            throw new IllegalArgumentException("expression must evaluate to a boolean");
        }
    }
}
