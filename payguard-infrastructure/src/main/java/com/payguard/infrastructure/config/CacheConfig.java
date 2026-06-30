package com.payguard.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache abstraction'ı etkinleştirir (@Cacheable/@CacheEvict çalışsın diye).
 *
 * Hangi sağlayıcının kullanılacağı (simple/redis) application.yml + profil ile belirlenir;
 * kod sağlayıcıdan bağımsızdır — sadece anotasyonlarla çalışır.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
