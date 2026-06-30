package com.payguard.infrastructure.tenant;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 'multitenant' profili aktifken DataSource'u tenant'a göre yönlendiren yapılandırma.
 *
 * Profil kapalıyken bu sınıf devre dışıdır → Spring Boot'un tek-DB otomatik yapılandırması kullanılır.
 *
 * Şema yönetimi artık her tenant DB'sine ayrı ayrı uygulanır (bkz. {@link MultiTenantFlywayMigrator}).
 */
@Configuration
@Profile("multitenant")
public class MultiTenantDataSourceConfig {

    /**
     * Tüm tenant DataSource'ları (varsayılan dahil). Tek yerde üretilir; hem routing hem migrator kullanır.
     * NOT: Map<String,DataSource> tipinde TEK bir bean'dir; içindeki DataSource'lar ayrı bean DEĞİL —
     * böylece JPA için tek (@Primary) DataSource kalır, ambiguity olmaz.
     */
    @Bean
    public Map<String, DataSource> tenantDataSources() {
        Map<String, DataSource> map = new LinkedHashMap<>();
        map.put("default", h2("payguard_default"));
        map.put("alpha", h2("payguard_alpha"));
        map.put("beta", h2("payguard_beta"));
        return map;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<String, DataSource> tenants = tenantDataSources();   // @Configuration proxy → aynı singleton

        Map<Object, Object> targets = new java.util.HashMap<>();
        targets.put("alpha", tenants.get("alpha"));
        targets.put("beta", tenants.get("beta"));

        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        routing.setDefaultTargetDataSource(tenants.get("default"));   // tenant yoksa buraya
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
