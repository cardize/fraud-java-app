package com.fraud.api.security;

import com.fraud.application.tenant.TenantProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter that validates the Bearer token on every request and puts the identity into the
 * SecurityContext. A blacklisted (logged-out) token is rejected even if its signature is valid.
 *
 * SECURITY (cross-tenant isolation): the token's "tenant" claim must match the request's active
 * tenant (set by TenantFilter from the X-Tenant header). A valid token issued under tenant
 * "alpha" replayed with "X-Tenant: beta" is rejected with 403 — the header alone must never be
 * able to reroute an authenticated caller into another tenant's database.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklist blacklist;
    private final TenantProvider tenantProvider;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist blacklist,
                                   TenantProvider tenantProvider) {
        this.jwtService = jwtService;
        this.blacklist = blacklist;
        this.tenantProvider = tenantProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.validate(header.substring(7));
            if (claims != null && !blacklist.isRevoked(claims.getId())) {
                // Tenant binding: a null claim (tokens predating the claim) counts as "default".
                String tokenTenant = claims.get("tenant", String.class);
                if (tokenTenant == null) {
                    tokenTenant = "default";
                }
                if (!tokenTenant.equals(tenantProvider.currentTenant())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"success\":false,\"data\":null,\"message\":\"Token was not issued for this tenant\"}");
                    return;
                }
                // RBAC: authorities come from the token's "roles" claim (set at login from the
                // user store). Older/foreign tokens without the claim get no authorities.
                List<?> roles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = roles == null ? List.of()
                        : roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
                var auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
