package com.payguard.infrastructure.tenant;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 'multitenant' profili aktifken DataSource'u tenant'a göre yönlendiren yapılandırma.
 *
 * .NET karşılığı: tenant başına ayrı connection string (TenantDatabaseSettings / PayGuardDbContext seçimi).
 * Profil kapalıyken bu sınıf devre dışıdır → Spring Boot'un tek-DB otomatik yapılandırması kullanılır
 * (varsayılan/test çalışması etkilenmez).
 *
 * SINIRLAMA (öğrenme dilimi): Flyway/Hibernate şema oluşturma yalnızca varsayılan hedefe uygulanır.
 * Gerçek çok-kiracıda her tenant DB'sine ayrı migration koşulur (Flyway'i her DataSource için tetikleyerek).
 */
@Configuration
@Profile("multitenant")
public class MultiTenantDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        // Demo: iki tenant + varsayılan, hepsi ayrı H2 in-memory DB.
        // Üretimde bu harita konfigürasyondan/secret'tan (tenant -> connection string) doldurulur.
        DataSource defaultDs = h2("payguard_default");
        DataSource alpha = h2("payguard_alpha");
        DataSource beta = h2("payguard_beta");

        Map<Object, Object> targets = new HashMap<>();
        targets.put("alpha", alpha);
        targets.put("beta", beta);

        TenantRoutingDataSource routing = new TenantRoutingDataSource();
        routing.setDefaultTargetDataSource(defaultDs);   // tenant yoksa buraya gider
        routing.setTargetDataSources(targets);
        routing.afterPropertiesSet();
        return routing;
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
