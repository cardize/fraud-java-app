package com.payguard.domain.rule;

/**
 * Kural tipi — her tip için ayrı bir değerlendirici (executor) stratejisi olabilir.
 *
 * .NET karşılığı: PayGRulesEngine/Rules altındaki RuleType (Simple, Complex, Linked, Periodic, RuleSet).
 * Dikey dilimde SIMPLE'a odaklanıyoruz; diğerleri için Strategy deseni hazır.
 */
public enum RuleType {
    SIMPLE,
    COMPLEX,
    LINKED,
    PERIODIC,
    RULE_SET
}
