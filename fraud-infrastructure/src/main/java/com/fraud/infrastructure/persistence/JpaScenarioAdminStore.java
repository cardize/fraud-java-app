package com.fraud.infrastructure.persistence;

import com.fraud.application.scenarios.CreateScenarioCommand;
import com.fraud.application.scenarios.ScenarioAdminStore;
import com.fraud.application.scenarios.dto.ScenarioDto;
import com.fraud.domain.rule.RuleType;
import com.fraud.infrastructure.rules.ScenarioCatalog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JPA ADAPTER for the ScenarioAdminStore port.
 *
 * Every mutation ends with a scenario-cache eviction — the engine reads scenarios through the
 * cached ScenarioCatalog, so without eviction a change would only take effect after the cache
 * TTL (up to 10 minutes of decisions against the OLD rule set). Tying eviction to the mutation
 * here (instead of trusting each caller to remember) makes stale reads structurally impossible.
 */
@Component
public class JpaScenarioAdminStore implements ScenarioAdminStore {

    private final ScenarioJpaRepository repository;
    private final ScenarioCatalog catalog;

    public JpaScenarioAdminStore(ScenarioJpaRepository repository, ScenarioCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Override
    public ScenarioDto create(CreateScenarioCommand cmd) {
        ScenarioRow row = repository.save(new ScenarioRow(
                cmd.name(), cmd.productType(), cmd.module(), cmd.priority(),
                cmd.fraudResponseCode(), toRuleRows(cmd.rules())));
        catalog.evictAll();
        return toDto(row);
    }

    @Override
    public Optional<ScenarioDto> update(long id, CreateScenarioCommand data) {
        return repository.findById(id).map(row -> {
            row.replaceWith(data.name(), data.productType(), data.module(), data.priority(),
                    data.fraudResponseCode(), toRuleRows(data.rules()));
            ScenarioRow saved = repository.save(row);
            catalog.evictAll();
            return toDto(saved);
        });
    }

    @Override
    public boolean delete(long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id); // rules go with it (cascade + orphanRemoval)
        catalog.evictAll();
        return true;
    }

    @Override
    public List<ScenarioDto> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    private List<RuleRow> toRuleRows(List<CreateScenarioCommand.NewRule> rules) {
        List<RuleRow> rows = new ArrayList<>();
        for (CreateScenarioCommand.NewRule rule : rules) {
            rows.add(new RuleRow(rule.name(), RuleType.SIMPLE, rule.expression()));
        }
        return rows;
    }

    private ScenarioDto toDto(ScenarioRow row) {
        List<ScenarioDto.RuleDto> rules = row.getRules().stream()
                .map(r -> new ScenarioDto.RuleDto(r.getName(), r.getExpression()))
                .toList();
        return new ScenarioDto(row.getId(), row.getName(), row.getProductType().name(),
                row.getModule(), row.getPriority(), row.getFraudResponseCode(), rules);
    }
}
