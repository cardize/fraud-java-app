package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.infrastructure.persistence.AuditLogJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Read access to the audit trail (ADMIN only — enforced in SecurityConfig).
 * Returns the most recent 100 entries; a real deployment would add paging/filtering.
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "ADMIN only")
public class AuditController {

    public record AuditEntryDto(Instant occurredAt, String username, String action,
                                String detail, String correlationId) {}

    private final AuditLogJpaRepository repository;

    public AuditController(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "List the most recent audit entries", description = "ADMIN only. "
            + "Logins, token refreshes, logout and scenario mutations, newest first (max 100).")
    public ApiResult<List<AuditEntryDto>> latest() {
        List<AuditEntryDto> entries = repository.findTop100ByOrderByOccurredAtDesc().stream()
                .map(a -> new AuditEntryDto(a.getOccurredAt(), a.getUsername(), a.getAction(),
                        a.getDetail(), a.getCorrelationId()))
                .toList();
        return ApiResult.ok(entries);
    }
}
