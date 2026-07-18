package com.fraud.api.tenant;

import com.fraud.infrastructure.tenant.TenantCatalog;
import com.fraud.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * On every request, reads the tenant from the 'X-Tenant' header into TenantContext, and clears it
 * when the request finishes. High priority so it runs BEFORE the security filter.
 *
 * SECURITY (hardened after external review):
 *  - The header is only honored when the multitenant profile is active (TenantCatalog bean
 *    exists) AND names a KNOWN tenant — an arbitrary client-supplied value must not reach the
 *    routing DataSource, cache keys, or the JWT tenant claim. Unknown tenants get 400.
 *  - This filter alone does NOT authorize the tenant: it only establishes the request's tenant
 *    context. The binding between the CALLER and the tenant is enforced later by
 *    JwtAuthenticationFilter, which rejects tokens whose "tenant" claim doesn't match this
 *    context (the original design trusted the header entirely — a cross-tenant bypass).
 *  - In single-tenant mode the header is ignored outright.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantCatalog catalog; // null outside the multitenant profile

    public TenantFilter(ObjectProvider<TenantCatalog> catalogProvider) {
        this.catalog = catalogProvider.getIfAvailable();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenant = request.getHeader("X-Tenant");
            if (catalog != null && tenant != null && !tenant.isBlank()) {
                if (!catalog.tenants().contains(tenant)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"data\":null,\"message\":\"Unknown tenant\"}");
                    return;
                }
                TenantContext.set(tenant);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();   // no leakage across the thread pool
        }
    }
}
