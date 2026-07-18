package com.fraud.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Public endpoints: login, Swagger UI, actuator. Everything else requires a token.
 *
 * CORRECTED ASSUMPTION (found on the first real docker-compose run — MockMvc tests never exercise
 * a real second port, so this was never actually verified): management.server.port gives actuator
 * a separate embedded-Tomcat CONNECTOR, but it is still the SAME Tomcat instance and the SAME
 * servlet-container-wide Spring Security filter. apiChain has no securityMatcher (it matches
 * "/**"), so it intercepted /actuator/health on port 9090 too and returned 403 for every
 * unauthenticated scrape — Prometheus would have silently never worked, exactly the failure mode
 * Section 5.8 of the technical report describes, just via a different path than assumed. Fix:
 * actuator paths are explicitly permitAll()'d below; the port separation still matters for network
 * isolation (firewall/VPC), just not for bypassing this filter chain.
 */
@Configuration
public class SecurityConfig {

    /**
     * H2 console only — highest priority, sameOrigin framing allowed (the console uses an iframe).
     *
     * REGISTERED ONLY WHEN THE CONSOLE IS ENABLED: PathRequest.toH2Console() resolves the
     * H2ConsoleProperties bean lazily AT REQUEST TIME — with the console disabled (e.g. the
     * docker-compose Postgres deployment sets SPRING_H2_CONSOLE_ENABLED=false) that bean doesn't
     * exist, and every single request died with NoSuchBeanDefinitionException (HTTP 500) inside
     * the security chain. Found by the first real docker-compose run.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
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
                                        @Value("${fraud.security.login-rate-limit.window-seconds:60}") int rateWindowSeconds,
                                        @Value("${fraud.security.login-rate-limit.trusted-proxies:}") String trustedProxiesCsv)
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
                        // Actuator: ONLY the read-only endpoints actually exposed (see
                        // management.endpoints.web.exposure.include) are permitted — a blanket
                        // /actuator/** would silently open any endpoint someone later adds to the
                        // exposure list (env, heapdump...) and depends on the management port
                        // staying separate. Explicit list = safe even if actuator ever ends up on 8080.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info",
                                "/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus").permitAll()
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
                // trusted-proxies: only these peer addresses may supply X-Forwarded-For (see LoginRateLimitFilter).
                .addFilterBefore(new LoginRateLimitFilter(rateCapacity, Duration.ofSeconds(rateWindowSeconds),
                                parseCsv(trustedProxiesCsv)),
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

    private static java.util.Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
