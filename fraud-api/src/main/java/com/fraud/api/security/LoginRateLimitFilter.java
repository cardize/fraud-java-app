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

/**
 * Per-IP token-bucket rate limiting for the login endpoint (brute-force protection).
 *
 * SECURITY: Authentication used to be attemptable without limit (a real brute-force gap).
 * A separate bucket is kept per client IP: 5 attempts per minute, 429 beyond that.
 * Buckets live in a bounded/expiring Caffeine cache (no memory leak).
 *
 * Deliberately NOT a Spring bean — it is added to the chain manually inside SecurityConfig;
 * otherwise Spring Boot would register it twice, once in the security chain and once as an
 * auto-registered servlet filter.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";

    private final int capacity;
    private final Duration window;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public LoginRateLimitFilter(int capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
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

    /** Reads the real client IP from X-Forwarded-For when behind a reverse proxy/gateway. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
