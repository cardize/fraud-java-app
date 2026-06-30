package com.payguard.infrastructure.persistence;

import com.payguard.domain.rule.RuleType;
import com.payguard.domain.shared.ProductType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Başlangıçta örnek senaryoları DB'ye ekler (tablo boşsa).
 *
 * In-memory DB her açılışta boş başladığından, akışı uçtan uca denenebilir kılmak için seed ediyoruz.
 * Üretimde bu sınıf kaldırılır; senaryolar yönetim arayüzünden girilir.
 */
@Component
public class ScenarioSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScenarioSeeder.class);

    private final ScenarioJpaRepository repository;

    public ScenarioSeeder(ScenarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (repository.existsByProductType(ProductType.CARD)) {
            return; // zaten yüklenmiş
        }

        // CARD senaryoları
        ScenarioRow highAmount = new ScenarioRow(
                "Yüksek Tutar", ProductType.CARD, 1, 1, "REJECT",
                List.of(new RuleRow("Tutar eşiği aşıyor", RuleType.SIMPLE, "amount.doubleValue() > threshold")));

        ScenarioRow nightLargeSpend = new ScenarioRow(
                "Gece Yüksek Harcama", ProductType.CARD, 1, 2, "REVIEW",
                List.of(
                        new RuleRow("Gece saati", RuleType.SIMPLE, "hourOfDay >= 0 and hourOfDay < 6"),
                        new RuleRow("Tutar > 1000", RuleType.SIMPLE, "amount.doubleValue() > 1000")));

        // PF için örnek bir senaryo (yeni ürün tipinin de DB'den çalıştığını göstermek için)
        ScenarioRow pfHighAmount = new ScenarioRow(
                "PF Yüksek Tutar", ProductType.PF, 1, 1, "REJECT",
                List.of(new RuleRow("PF tutar > 10000", RuleType.SIMPLE, "amount.doubleValue() > 10000")));

        repository.saveAll(List.of(highAmount, nightLargeSpend, pfHighAmount));
        log.info("Örnek senaryolar yüklendi: {} adet", repository.count());
    }
}
