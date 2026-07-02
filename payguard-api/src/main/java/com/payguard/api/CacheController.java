package com.payguard.api;

import com.payguard.application.common.ApiResult;
import com.payguard.infrastructure.rules.ScenarioCatalog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cache management endpoints. Used to clear the cache when rules/scenarios change.
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
        return ApiResult.ok("Scenario cache cleared");
    }
}
