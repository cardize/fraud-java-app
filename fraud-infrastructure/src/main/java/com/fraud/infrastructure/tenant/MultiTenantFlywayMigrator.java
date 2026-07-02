package com.fraud.infrastructure.tenant;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Applies Flyway migrations to each tenant DataSource separately.
 *
 * In multi-tenant mode, Spring Boot's single automatic Flyway run isn't enough (it only targets
 * the primary DataSource). This class walks every tenant DB at @PostConstruct and calls
 * Flyway.migrate() programmatically for each -> every tenant's schema gets created.
 *
 * Only produced as a bean by MultiTenantDataSourceConfig, and only in the 'multitenant' profile.
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
            log.info("Flyway migration complete: tenant={}", tenant);
        });
    }
}
