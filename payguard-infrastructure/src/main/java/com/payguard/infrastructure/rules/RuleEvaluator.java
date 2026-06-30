package com.payguard.infrastructure.rules;

import com.payguard.application.fraud.FraudParameters;
import com.payguard.domain.rule.Rule;
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
 * Tek bir kuralı FraudParameters üzerinde değerlendirir.
 *
 * GÜVENLİK: Kural ifadeleri verisel kaynaklıdır; bu yüzden tam yetkili StandardEvaluationContext yerine
 * yalnızca property okuma + operatörlere izin veren SimpleEvaluationContext kullanılır. Bu, ifade içinden
 * Java tipi/metot/constructor çağrısını (örn. T(Runtime).exec(...)) engeller → SpEL injection kapatılmıştır.
 *
 * PERFORMANS: İfadeler parse edilip cache'lenir; SpEL derleyici (MIXED) sık çalışan ifadeleri
 * bytecode'a çevirir, derlenemezse güvenle yorumlanmış moda döner.
 */
@Component
public class RuleEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.MIXED, getClass().getClassLoader()));
    private final ConcurrentHashMap<String, Expression> cache = new ConcurrentHashMap<>();

    public boolean evaluate(Rule rule, FraudParameters params) {
        Expression expression = cache.computeIfAbsent(rule.expression(), parser::parseExpression);

        // Salt-okunur bağlam: yalnızca params'ın property'leri (amountValue, threshold, hourOfDay...) okunabilir.
        EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(params)
                .build();

        Boolean result = expression.getValue(context, Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
