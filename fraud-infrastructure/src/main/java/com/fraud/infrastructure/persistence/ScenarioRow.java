package com.fraud.infrastructure.persistence;

import com.fraud.domain.shared.ProductType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario table row (JPA entity / persistence model).
 *
 * A scenario has MULTIPLE rules -> @OneToMany.
 */
@Entity
@Table(name = "scenarios")
public class ScenarioRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private int module;
    private int priority;
    private String fraudResponseCode;

    // Rules are managed along with the scenario (cascade) on add/delete. FK: scenario_id
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "scenario_id")
    private List<RuleRow> rules = new ArrayList<>();

    protected ScenarioRow() {
    }

    public ScenarioRow(String name, ProductType productType, int module,
                       int priority, String fraudResponseCode, List<RuleRow> rules) {
        this.name = name;
        this.productType = productType;
        this.module = module;
        this.priority = priority;
        this.fraudResponseCode = fraudResponseCode;
        this.rules = rules;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProductType getProductType() { return productType; }
    public int getModule() { return module; }
    public int getPriority() { return priority; }
    public String getFraudResponseCode() { return fraudResponseCode; }
    public List<RuleRow> getRules() { return rules; }

    /**
     * Full replacement (PUT semantics): all fields AND the whole rule list are replaced.
     * The old RuleRows are deleted by orphanRemoval; the new ones are cascaded in. The managed
     * collection is mutated IN PLACE (clear+addAll) — swapping the list reference would break
     * Hibernate's collection tracking.
     */
    public void replaceWith(String name, ProductType productType, int module,
                            int priority, String fraudResponseCode, List<RuleRow> newRules) {
        this.name = name;
        this.productType = productType;
        this.module = module;
        this.priority = priority;
        this.fraudResponseCode = fraudResponseCode;
        this.rules.clear();
        this.rules.addAll(newRules);
    }
}
