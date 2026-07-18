package com.fraud.api.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Per-IP token-bucket rate limiting for the login endpoint (brute-force protection).
 *
 * SECURITY: Authentication used to be attemptable without limit (a real brute-force gap).
 * A separate bucket is kept per client IP: 5 attempts per minute, 429 beyond that.
 * Buckets live in a bounded/expiring Caffeine cache (no memory leak).
 *
 * NOTE (single-instance scope): buckets are in-process. In a horizontally scaled deployment an
 * attacker gets capacity × instances, and limits reset on restart — move the buckets to a shared
 * store (e.g. Redis via bucket4j-redis) before scaling out.
 *
 * Deliberately NOT a Spring bean — it is added to the chain manually inside SecurityConfig;
 * otherwise Spring Boot would register it twice, once in the security chain and once as an
 * auto-registered servlet filter.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final int capacity;
    private final Duration window;
    private final Set<String> trustedProxies;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public LoginRateLimitFilter(int capacity, Duration window, Set<String> trustedProxies) {
        this.capacity = capacity;
        this.window = window;
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!LOGIN_PATH.equals(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.get(clientIp(request), key -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"data\":null,\"message\":\"Too many attempts — please try again later\"}");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, window));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * The bucket key: the direct peer address, unless that peer is a TRUSTED reverse proxy — only
     * then is X-Forwarded-For consulted.
     *
     * SECURITY (fixed after external review): the original version trusted XFF from ANYONE.
     * Since /login is public, an attacker connecting directly could send a different fabricated
     * XFF value on every request, get a fresh bucket each time, and brute-force without ever
     * hitting the limit. Now a spoofed XFF from an untrusted peer is simply ignored — all of that
     * attacker's requests share the bucket of their real socket address. Trusted proxies are
     * configured via fraud.security.login-rate-limit.trusted-proxies (empty by default: never
     * trust XFF).
     */
    private String clientIp(HttpServletRequest request) {
        if (trustedProxies.contains(request.getRemoteAddr())) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
