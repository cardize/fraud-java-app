package com.fraud.infrastructure.rules;

import com.fraud.domain.rule.Rule;
import com.fraud.domain.rule.Scenario;
import com.fraud.domain.shared.ProductType;
import com.fraud.infrastructure.persistence.RuleRow;
import com.fraud.infrastructure.persistence.ScenarioJpaRepository;
import com.fraud.infrastructure.persistence.ScenarioRow;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads scenarios from the DB and maps the persistence model (ScenarioRow/RuleRow) to the domain
 * model (Scenario/Rule records).
 */
@Component
public class ScenarioCatalog {

    private final ScenarioJpaRepository scenarioJpaRepository;

    public ScenarioCatalog(ScenarioJpaRepository scenarioJpaRepository) {
        this.scenarioJpaRepository = scenarioJpaRepository;
    }

    /**
     * Loads scenarios; the result is cached under "scenarios" (keyed by tenant + product type +
     * module) so we don't hit the DB on every transaction.
     *
     * SECURITY/BUGFIX: The previous key was productType+module only — it did NOT include the
     * TENANT. In the multitenant profile, this caused cross-tenant cache poisoning: the rule set
     * loaded for Tenant A would be cached, and the very next Tenant B request would get a cache
     * hit under the SAME key (e.g. "CARD-1") and have A's rules applied to B's transaction (a
     * wrong fraud decision plus a possible compliance violation). The key now also includes the
     * current tenant read from TenantContext.
     */
    @Cacheable(value = "scenarios",
            key = "T(com.fraud.infrastructure.tenant.TenantContext).currentOrDefault() + '-' + #productType + '-' + #module")
    public List<Scenario> loadOnlineScenarios(ProductType productType, int module) {
        return scenarioJpaRepository.findByProductTypeAndModule(productType, module).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Clears the entire scenario cache (called when rules/scenarios change).
     */
    @CacheEvict(value = "scenarios", allEntries = true)
    public void evictAll() {
    }

    private Scenario toDomain(ScenarioRow row) {
        List<Rule> rules = row.getRules().stream()
                .map(this::toDomain)
                .toList();
        return new Scenario(row.getId(), row.getName(), row.getPriority(),
                row.getFraudResponseCode(), rules);
    }

    private Rule toDomain(RuleRow row) {
        return new Rule(row.getId(), row.getName(), row.getType(), row.getExpression());
    }
}
