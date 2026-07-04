package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.infrastructure.persistence.AuditLogJpaRepository;
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
public class AuditController {

    public record AuditEntryDto(Instant occurredAt, String username, String action,
                                String detail, String correlationId) {}

    private final AuditLogJpaRepository repository;

    public AuditController(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResult<List<AuditEntryDto>> latest() {
        List<AuditEntryDto> entries = repository.findTop100ByOrderByOccurredAtDesc().stream()
                .map(a -> new AuditEntryDto(a.getOccurredAt(), a.getUsername(), a.getAction(),
                        a.getDetail(), a.getCorrelationId()))
                .toList();
        return ApiResult.ok(entries);
    }
}
