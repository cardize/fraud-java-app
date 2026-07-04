package com.fraud.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Duration;

/**
 * Security configuration — stateless JWT.
 *
 * Two separate filter chains: the H2 console is isolated in its own chain (frame sameOrigin) so
 * the MAIN chain keeps its secure headers: clickjacking protection (frame DENY), HSTS and
 * referrer-policy.
 *
 * Public endpoints: login, Swagger UI. Everything else requires a token.
 * Actuator (health/metrics/prometheus) is NOT covered by this chain — it is served on a separate
 * port (management.server.port), see application.yml.
 */
@Configuration
public class SecurityConfig {

    /** H2 console only — highest priority, sameOrigin framing allowed (the console uses an iframe). */
    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /** Main application chain — JWT + secure headers. */
    @Bean
    public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                        @Value("${fraud.security.login-rate-limit.capacity:5}") int rateCapacity,
                                        @Value("${fraud.security.login-rate-limit.window-seconds:60}") int rateWindowSeconds)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                       // stateless API
                .headers(h -> h
                        .frameOptions(frame -> frame.deny())                 // clickjacking protection
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))               // 1 year (effective under HTTPS)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // RBAC: cache and scenario mutations change engine behavior for everyone
                        // -> admins only. Reading scenarios (GET) stays open to any authenticated
                        // user. Roles come from the JWT's "roles" claim (see JwtAuthenticationFilter).
                        .requestMatchers("/api/v1/cache/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/scenarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/scenarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/scenarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // The rate limiter runs first (even before JWT validation) so limit overruns are rejected cheaply.
                .addFilterBefore(new LoginRateLimitFilter(rateCapacity, Duration.ofSeconds(rateWindowSeconds)),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * BCrypt with a per-password random salt (default strength 10). Deliberately slow — that cost
     * is the defense against offline hash cracking. Used by login verification and the user seeder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
