package com.payguard.infrastructure.tenant;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Her tenant DataSource'una ayrı ayrı Flyway migration uygular.
 *
 * Çok-kiracıda Spring Boot'un tek otomatik Flyway'i yetmez (sadece primary DataSource'a koşar).
 * Bu sınıf @PostConstruct'ta tüm tenant DB'lerini gezip programatik Flyway.migrate() çağırır
 * → her tenant şeması kurulur.
 *
 * Sadece 'multitenant' profilinde, MultiTenantDataSourceConfig tarafından bean olarak üretilir.
 */
public class MultiTenantFlywayMigrator {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantFlywayMigrator.class);

    private final Map<String, DataSource> tenantDataSources;

    public MultiTenantFlywayMigrator(Map<String, DataSource> tenantDataSources) {
        this.tenantDataSources = tenantDataSources;
    }

    @PostConstruct
    public void migrateAll() {
        tenantDataSources.forEach((tenant, dataSource) -> {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            log.info("Flyway migration tamamlandı: tenant={}", tenant);
        });
    }
}
