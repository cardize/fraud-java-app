package com.payguard.domain.rule;

import java.io.Serializable;

/**
 * Tek bir kural. SpEL (Spring Expression Language) ifadesi tutar.
 *
 * .NET karşılığı: PayGuard.Domain/Entities/Rule.cs + RuleInfo (kural motoru modeli).
 * .NET'te Microsoft RulesEngine "lambda expression" string'i değerlendiriyordu;
 * Java tarafında bunun en yakın yerleşik karşılığı SpEL'dir.
 *
 * @param id            kural kimliği
 * @param name          okunabilir ad
 * @param type          kural tipi (değerlendirici stratejisini seçer)
 * @param expression    SpEL ifadesi; FraudParameters üzerinde true dönerse kural "hit" olur
 *                      örn: "amount > threshold" veya "hourOfDay < 6 and amount > 5000"
 */
public record Rule(
        long id,
        String name,
        RuleType type,
        String expression
) implements Serializable {
}
