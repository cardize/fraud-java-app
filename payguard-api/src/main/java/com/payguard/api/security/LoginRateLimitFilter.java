package com.payguard.api.security;

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
 * Login uç noktasına IP başına token-bucket hız sınırlaması (brute-force koruması).
 *
 * GÜVENLİK: Doğrulama daha önce sınırsız denenebiliyordu (gerçek bir kaba kuvvet açığı).
 * Her istemci IP'si için ayrı bir bucket tutulur: dakikada 5 deneme, üzeri 429 döner.
 * Bucket'lar sınırlı/expire olan bir Caffeine cache'inde tutulur (bellek sızıntısı olmaz).
 *
 * Bilinçli olarak Spring bean'i DEĞİL — SecurityConfig içinde elle zincire eklenir; aksi halde
 * Spring Boot bunu hem security zincirine hem otomatik servlet filter olarak iki kez kaydederdi.
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
                    "{\"success\":false,\"data\":null,\"message\":\"Çok fazla deneme — lütfen sonra tekrar deneyin\"}");
        }
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, window));
        return Bucket.builder().addLimit(limit).build();
    }

    /** Ters proxy/gateway arkasında gerçek istemci IP'sini X-Forwarded-For'dan okur. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
