package com.fraud.infrastructure.persistence;

import com.fraud.application.common.PageResult;
import com.fraud.application.scenarios.CreateScenarioCommand;
import com.fraud.application.scenarios.ListScenariosCommand;
import com.fraud.application.scenarios.ScenarioAdminStore;
import com.fraud.application.scenarios.dto.ScenarioDto;
import com.fraud.domain.rule.RuleType;
import com.fraud.infrastructure.rules.ScenarioCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public PageResult<ScenarioDto> list(ListScenariosCommand query) {
        // Optional filters become predicates ONLY when present (criteria composition instead of
        // "(:p is null or ...)" JPQL, which is database-dependent for typed nulls).
        Specification<ScenarioRow> spec = (root, q, cb) -> cb.conjunction();
        if (query.productType() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("productType"), query.productType()));
        }
        if (query.module() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("module"), query.module()));
        }
        Page<ScenarioRow> page = repository.findAll(spec,
                PageRequest.of(query.page(), query.size(), Sort.by("id")));
        return new PageResult<>(
                page.getContent().stream().map(this::toDto).toList(),
                query.page(), query.size(), page.getTotalElements(), page.getTotalPages());
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
