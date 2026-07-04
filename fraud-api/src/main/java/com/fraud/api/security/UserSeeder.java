package com.fraud.api.security;

import com.fraud.infrastructure.persistence.UserAccount;
import com.fraud.infrastructure.persistence.UserAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds default users at startup (if the users table is empty).
 *
 * Lives in the API module (not infrastructure) because it needs the PasswordEncoder bean —
 * encoding is a composition-root concern; infrastructure stores only the finished hash.
 *
 * - admin   (roles: ADMIN,USER) — full access, including /api/v1/cache/**
 * - analyst (roles: USER)       — regular endpoints only
 *
 * The embedded default passwords are DEV-ONLY; production must override them via the
 * FRAUD_ADMIN_PASSWORD / FRAUD_ANALYST_PASSWORD env variables.
 */
@Component
public class UserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserAccountJpaRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String analystPassword;

    public UserSeeder(UserAccountJpaRepository users,
                      PasswordEncoder passwordEncoder,
                      @Value("${fraud.security.seed-users.admin-password:fraud123}") String adminPassword,
                      @Value("${fraud.security.seed-users.analyst-password:analyst123}") String analystPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.analystPassword = analystPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfEmpty();
    }

    /**
     * Idempotent seeding, extracted so it can also be invoked PER TENANT (multitenant profile,
     * see MultiTenantSeeder). Without that, only the default tenant DB would have users and a
     * login with an X-Tenant header would always fail against an empty users table.
     */
    public void seedIfEmpty() {
        if (users.count() > 0) {
            return; // already seeded (or managed externally)
        }
        users.saveAll(List.of(
                new UserAccount("admin", passwordEncoder.encode(adminPassword), List.of("ADMIN", "USER")),
                new UserAccount("analyst", passwordEncoder.encode(analystPassword), List.of("USER"))));
        log.warn("Seeded default users (admin, analyst). The embedded passwords are dev-only — "
                + "override via FRAUD_ADMIN_PASSWORD / FRAUD_ANALYST_PASSWORD in production.");
    }
}
