package com.fraud.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the cache abstraction (so @Cacheable/@CacheEvict work).
 *
 * Which provider (simple/redis) is used is decided by application.yml + profile;
 * the code is provider-agnostic — it works purely through the annotations.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
