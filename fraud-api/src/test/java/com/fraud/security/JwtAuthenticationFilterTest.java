package com.fraud.security;

import com.fraud.api.security.JwtAuthenticationFilter;
import com.fraud.api.security.JwtService;
import com.fraud.api.security.TokenBlacklist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Locks in the tenant-binding behavior added for external review finding A: a syntactically valid
 * token must only authenticate a request whose active tenant matches the token's "tenant" claim.
 * The multitenant profile is never active in the MockMvc suite, so this is the only place the
 * mismatch path gets exercised — the filter is driven directly with mock servlet objects and a
 * stubbed TenantProvider standing in for the request's routed tenant.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-characters-long";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);
    private final TokenBlacklist blacklist = new TokenBlacklist(3_600_000);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenIssuedForTheActiveTenantAuthenticates() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, blacklist, () -> "alpha");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.issue("alice", List.of("USER"), "alpha"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void tokenIssuedForAnotherTenantIsRejectedWith403() throws Exception {
        // Request routed to tenant "beta" (X-Tenant header via TenantFilter), but the token was
        // minted during a login against "alpha" — the exact cross-tenant replay from the review.
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, blacklist, () -> "beta");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.issue("alice", List.of("USER"), "alpha"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "a cross-tenant token must never establish an authentication");
    }

    @Test
    void legacyTokenWithoutTenantClaimCountsAsDefaultTenant() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, blacklist, () -> "default");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.issue("alice", List.of("USER"), "default"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
