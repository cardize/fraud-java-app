package com.payguard.infrastructure.persistence;

import com.payguard.domain.rule.RuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Kural tablosu satırı (JPA entity / persistence modeli).
 *
 * .NET karşılığı: PayGuard.Domain/Entities/Rule.cs + RuleEntityConfiguration (EF Fluent mapping).
 * Bilinçli ayrım: bu PERSISTENCE modeli; domain'deki {@link com.payguard.domain.rule.Rule}
 * record'u ise saf İŞ modeli. ScenarioCatalog ikisini birbirine map'ler.
 */
@Entity
@Table(name = "rules")
public class RuleRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private RuleType type;

    @Column(length = 1000)
    private String expression;

    protected RuleRow() {
    }

    public RuleRow(String name, RuleType type, String expression) {
        this.name = name;
        this.type = type;
        this.expression = expression;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public RuleType getType() { return type; }
    public String getExpression() { return expression; }
}
