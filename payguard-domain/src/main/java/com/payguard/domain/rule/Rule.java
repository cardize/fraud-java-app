package com.payguard.domain.rule;

import java.io.Serializable;

/**
 * Tek bir kural. Değerlendirme için SpEL (Spring Expression Language) ifadesi tutar.
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
