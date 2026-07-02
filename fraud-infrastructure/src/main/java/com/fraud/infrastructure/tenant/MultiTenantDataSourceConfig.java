package com.fraud.infrastructure.tenant;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration that routes the DataSource by tenant while the 'multitenant' profile is active.
 *
 * This class is inactive when the profile is off -> Spring Boot's single-DB auto-configuration is used.
 *
 * Schema management is now applied to each tenant DB separately (see {@link MultiTenantFlywayMigrator}).
 */
@Configuration
@Profile("multitenant")
public class MultiTenantDataSourceConfig {

    /**
     * All tenant DataSources (including the default one). Produced in a single place; used by
     * both routing and the migrator.
     * NOTE: this is a SINGLE bean of type Map<String,DataSource> — the DataSources inside it are
     * NOT separate beans — so JPA still gets exactly one (@Primary) DataSource, with no ambiguity.
     */
    @Bean
    public Map<String, DataSource> tenantDataSources() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        map.put("default", h2("fraud_default"));
        map.put("alpha", h2("fraud_alpha"));
        map.put("beta", h2("fraud_beta"));
        return map;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<String, DataSource> tenants = tenantDataSources();   // @Configuration proxy -> same singleton

        Map<Object, Object> targets = new java.util.HashMap<>();
        targets.put("alpha", tenants.get("alpha"));
        targets.put("beta", tenants.get("beta"));

        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        routing.setDefaultTargetDataSource(tenants.get("default"));   // used when no tenant is set
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean
    public MultiTenantFlywayMigrator multiTenantFlywayMigrator() {
        return new MultiTenantFlywayMigrator(tenantDataSources());
    }

    private DataSource h2(String dbName) {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .build();
    }
}
