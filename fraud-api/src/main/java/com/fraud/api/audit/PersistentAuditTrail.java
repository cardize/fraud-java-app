package com.fraud.api.audit;

import com.fraud.api.CorrelationIdFilter;
import com.fraud.application.audit.AuditTrail;
import com.fraud.infrastructure.persistence.AuditLog;
import com.fraud.infrastructure.persistence.AuditLogJpaRepository;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * ADAPTER for the AuditTrail port.
 *
 * Lives in the API module (composition root) because resolving WHO acted requires the Spring
 * Security context, which neither application nor infrastructure may depend on. The correlation
 * id is taken from the MDC (put there by CorrelationIdFilter), so an audit row can be joined with
 * the request's log lines.
 */
@Component
public class PersistentAuditTrail implements AuditTrail {

    private static final int MAX_DETAIL = 1000;

    private final AuditLogJpaRepository repository;

    public PersistentAuditTrail(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(String action, String detail) {
        String safeDetail = detail != null && detail.length() > MAX_DETAIL
                ? detail.substring(0, MAX_DETAIL) : detail;
        repository.save(new AuditLog(currentUsername(), action, safeDetail,
                MDC.get(CorrelationIdFilter.MDC_KEY)));
    }

    /** The authenticated principal, or "anonymous" (e.g. a failed login attempt has no principal). */
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return "anonymous";
        }
        String name = String.valueOf(auth.getPrincipal());
        return "anonymousUser".equals(name) ? "anonymous" : name;
    }
}
