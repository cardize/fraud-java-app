package com.payguard.api.tenant;

import com.payguard.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Her istekte 'X-Tenant' header'ından kiracıyı okuyup TenantContext'e koyar, istek sonunda temizler.
 *
 * Tek-kiracı modunda zararsızdır (routing DataSource yoksa thread-local kullanılmaz).
 * Güvenlik filtresinden ÖNCE çalışsın diye yüksek öncelikli.
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
            TenantContext.clear();   // thread havuzunda sızıntı olmasın
        }
    }
}
