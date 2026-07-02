package com.payguard.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SHARED thread pool for scenario evaluation.
 *
 * Previously a new pool was opened and closed on every request (expensive and unable to scale).
 * This bean keeps a single pool for the whole application lifetime; its size is controlled by
 * payguard.scenario.max-parallelism. destroyMethod=shutdown closes it cleanly on application shutdown.
 */
@Configuration
public class ScenarioExecutorConfig {

    @Bean(name = "scenarioExecutor", destroyMethod = "shutdown")
    public ExecutorService scenarioExecutor(
            @Value("${payguard.scenario.max-parallelism:10}") int maxParallelism) {
        return Executors.newFixedThreadPool(Math.max(1, maxParallelism));
    }
}
