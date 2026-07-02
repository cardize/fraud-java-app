package com.fraud.api.tenant;

import com.fraud.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * On every request, reads the tenant from the 'X-Tenant' header into TenantContext, and clears it
 * when the request finishes.
 *
 * Harmless in single-tenant mode (the thread-local is unused when there is no routing DataSource).
 * High priority so it runs BEFORE the security filter.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenant = request.getHeader("X-Tenant");
            if (tenant != null && !tenant.isBlank()) {
                TenantContext.set(tenant);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();   // no leakage across the thread pool
        }
    }
}
