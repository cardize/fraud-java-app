package com.payguard.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Senaryo değerlendirmesi için PAYLAŞIMLI thread havuzu.
 *
 * Daha önce her istekte yeni bir havuç açılıp kapatılıyordu (pahalı ve ölçeklenmez). Bu bean
 * uygulama boyunca tek bir havuz tutar; sınırı payguard.scenario.max-parallelism ile belirlenir.
 * destroyMethod=shutdown ile uygulama kapanırken düzgünce kapatılır.
 */
@Configuration
public class ScenarioExecutorConfig {

    @Bean(name = "scenarioExecutor", destroyMethod = "shutdown")
    public ExecutorService scenarioExecutor(
            @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        return Executors.newFixedThreadPool(Math.max(1, maxParallelism));
    }
}
