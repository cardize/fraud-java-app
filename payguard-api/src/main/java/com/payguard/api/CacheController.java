package com.payguard.api;

import com.payguard.infrastructure.rules.ScenarioCatalog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cache yönetimi uç noktaları.
 *
 * .NET karşılığı: PayGuard.Internal.API/Controllers/CacheController.cs (+ CacheSynchronization).
 * Kural/senaryo değiştiğinde cache'i temizlemek için kullanılır.
 */
@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

    private final ScenarioCatalog scenarioCatalog;

    public CacheController(ScenarioCatalog scenarioCatalog) {
        this.scenarioCatalog = scenarioCatalog;
    }

    @PostMapping("/evict-scenarios")
    public ApiResult<String> evictScenarios() {
        scenarioCatalog.evictAll();
        return ApiResult.ok("Senaryo cache temizlendi");
    }
}
