package com.payguard.api.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Güvenlik yapılandırması — stateless JWT.
 *
 * İki ayrı filtre zinciri: H2 console kendi zincirinde (frame sameOrigin) izole edilir; böylece
 * ANA zincir clickjacking koruması (frame DENY), HSTS ve referrer-policy gibi güvenli başlıkları korur.
 *
 * Açık (public) uçlar: login, actuator health, Swagger UI. Diğer her şey token ister.
 */
@Configuration
public class SecurityConfig {

    /** Sadece H2 console — yüksek öncelik, frame'e sameOrigin izni (konsol iframe kullanır). */
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

    /** Ana uygulama zinciri — JWT + güvenli başlıklar. */
    @Bean
    public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                       // stateless API
                .headers(h -> h
                        .frameOptions(frame -> frame.deny())                 // clickjacking koruması
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))               // 1 yıl (HTTPS altında etkin)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/health",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
