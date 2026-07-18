package com.fraud.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fails startup FAST when a production-like profile still runs on the committed dev defaults
 * (external review finding H).
 *
 * The dev JWT secret and seed passwords in application.yml are deliberately functional so the
 * demo runs out of the box — but that same convenience must never survive into production
 * silently. All dev defaults carry the "change-me" marker or are the documented demo passwords;
 * if any of them is still active while a "prod"/"production" profile is on, the application
 * refuses to boot with an explicit message instead of serving traffic with known credentials.
 */
@Component
public class SecretsSanityCheck {

    public SecretsSanityCheck(Environment environment,
                              @Value("${fraud.security.jwt-secret}") String jwtSecret,
                              @Value("${fraud.security.seed-users.admin-password:fraud123}") String adminPassword,
                              @Value("${fraud.security.seed-users.analyst-password:analyst123}") String analystPassword) {
        boolean prodLike = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        if (!prodLike) {
            return;
        }
        if (jwtSecret.contains("change-me")) {
            throw new IllegalStateException(
                    "Refusing to start: fraud.security.jwt-secret is still the committed dev default. "
                            + "Set FRAUD_JWT_SECRET for production.");
        }
        if ("fraud123".equals(adminPassword) || "analyst123".equals(analystPassword)) {
            throw new IllegalStateException(
                    "Refusing to start: seed user passwords are still the committed dev defaults. "
                            + "Set FRAUD_ADMIN_PASSWORD / FRAUD_ANALYST_PASSWORD for production.");
        }
    }
}
