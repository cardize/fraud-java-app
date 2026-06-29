package com.payguard.infrastructure.rules;

import com.payguard.application.fraud.FraudParameters;
import com.payguard.domain.rule.Rule;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tek bir kuralı FraudParameters üzerinde değerlendirir.
 *
 * .NET karşılığı: PayGRulesEngine/Rules/IRuleExecutor + ComplexRuleExecuter (Microsoft RulesEngine).
 * Burada SpEL kullanıyoruz: kural ifadesi "amount > threshold" gibi bir string;
 * FraudParameters'ın getter'ları ifade içinde değişken gibi erişilebilir olur.
 *
 * İfadeler derlenip cache'lenir (her çağrıda parse etmemek için) — performans dostu.
 */
@Component
public class RuleEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public boolean evaluate(Rule rule, FraudParameters params) {
        Expression expression = cache.computeIfAbsent(rule.expression(), parser::parseExpression);

        // rootObject = FraudParameters -> ifadede "amount", "threshold", "hourOfDay" doğrudan erişilir
        StandardEvaluationContext context = new StandardEvaluationContext(params);

        Boolean result = expression.getValue(context, Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
