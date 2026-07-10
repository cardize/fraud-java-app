package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.infrastructure.rules.ScenarioCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cache management endpoints. Used to clear the cache when rules/scenarios change.
 */
@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache", description = "ADMIN only")
public class CacheController {

    private final ScenarioCatalog scenarioCatalog;

    public CacheController(ScenarioCatalog scenarioCatalog) {
        this.scenarioCatalog = scenarioCatalog;
    }

    @PostMapping("/evict-scenarios")
    @Operation(summary = "Evict the scenario cache", description = "ADMIN only. The scenario "
            + "CRUD endpoints already evict automatically on every mutation — this is for "
            + "out-of-band changes (e.g. a direct DB edit) or manual troubleshooting.")
    public ApiResult<String> evictScenarios() {
        scenarioCatalog.evictAll();
        return ApiResult.ok("Scenario cache cleared");
    }
}
