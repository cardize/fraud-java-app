package com.fraud.infrastructure.rules;

import com.fraud.application.fraud.FraudParameters;
import com.fraud.domain.rule.Rule;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates a single rule against FraudParameters.
 *
 * SECURITY: Rule expressions come from data, so instead of the fully-privileged
 * StandardEvaluationContext, we use SimpleEvaluationContext, which only allows property reads and
 * operators. This blocks a Java type/method/constructor call from inside an expression (e.g.
 * T(Runtime).exec(...)) — SpEL injection is closed off.
 *
 * PERFORMANCE: Expressions are parsed and cached; the SpEL compiler (MIXED) turns frequently-run
 * expressions into bytecode and safely falls back to interpreted mode if it can't compile one.
 */
@Component
public class RuleEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.MIXED, getClass().getClassLoader()));
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public boolean evaluate(Rule rule, FraudParameters params) {
        Expression expression = cache.computeIfAbsent(rule.expression(), parser::parseExpression);

        // Read-only context: only params' properties (amountValue, threshold, hourOfDay...) can be read.
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(params)
                .build();

        Boolean result = expression.getValue(context, Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
