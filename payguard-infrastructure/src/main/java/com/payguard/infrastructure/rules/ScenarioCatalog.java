package com.payguard.infrastructure.rules;

import com.payguard.domain.rule.Rule;
import com.payguard.domain.rule.Scenario;
import com.payguard.domain.shared.ProductType;
import com.payguard.infrastructure.persistence.RuleRow;
import com.payguard.infrastructure.persistence.ScenarioJpaRepository;
import com.payguard.infrastructure.persistence.ScenarioRow;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Senaryoları DB'den yükler ve persistence modelini (ScenarioRow/RuleRow) domain modeline
 * (Scenario/Rule record) çevirir.
 *
 * .NET karşılığı: RuleDapperRepository'nin senaryo yükleme + DTO'ya map etme kısmı.
 * Artık sabit veri DEĞİL, gerçek tablodan okunur (önceki sürümdeki hard-coded liste kaldırıldı).
 */
@Component
public class ScenarioCatalog {

    private final ScenarioJpaRepository scenarioJpaRepository;

    public ScenarioCatalog(ScenarioJpaRepository scenarioJpaRepository) {
        this.scenarioJpaRepository = scenarioJpaRepository;
    }

    /**
     * Senaryoları yükler; sonuç "scenarios" cache'inde (ürün tipi + modül) saklanır.
     * .NET karşılığı: senaryo listesinin Redis/CacheList'te tutulması (her işlemde DB'ye gitmemek için).
     */
    @Cacheable(value = "scenarios", key = "#productType + '-' + #module")
    public List<Scenario> loadOnlineScenarios(ProductType productType, int module) {
        return scenarioJpaRepository.findByProductTypeAndModule(productType, module).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Tüm senaryo cache'ini temizler (kural/senaryo değişince çağrılır).
     * .NET karşılığı: CacheController + CacheSynchronization ile cache invalidation.
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
