package com.payguard.domain.rule;

/**
 * Kural tipi — her tip için ayrı bir değerlendirici (executor) stratejisi olabilir.
 * Şu an SIMPLE kullanılıyor; diğerleri için Strategy deseni hazır.
 */
public enum RuleType {
    SIMPLE,
    COMPLEX,
    LINKED,
    PERIODIC,
    RULE_SET
}
