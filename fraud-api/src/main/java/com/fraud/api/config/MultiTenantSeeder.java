package com.fraud.api.config;

import com.fraud.api.security.UserSeeder;
import com.fraud.infrastructure.persistence.ScenarioSeeder;
import com.fraud.infrastructure.tenant.TenantCatalog;
import com.fraud.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Tenant onboarding at startup (multitenant profile only).
 *
 * FIXES A KNOWN LIMITATION: the plain seeders are ApplicationRunners and execute with no
 * TenantContext set, so their writes are routed to the DEFAULT DataSource only — alpha/beta
 * stayed empty (no scenarios, and worse, NO USERS: a login with "X-Tenant: alpha" could never
 * succeed). This runner iterates every known tenant, sets the routing context, and runs the same
 * idempotent seeders against each tenant DB.
 *
 * The context is cleared in a finally block — this thread continues serving the application
 * afterwards, and a leaked tenant would silently route later work to the wrong DB.
 */
@Component
@Profile("multitenant")
public class MultiTenantSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantSeeder.class);

    private final TenantCatalog tenantCatalog;
    private final ScenarioSeeder scenarioSeeder;
    private final UserSeeder userSeeder;

    public MultiTenantSeeder(TenantCatalog tenantCatalog,
                             ScenarioSeeder scenarioSeeder,
                             UserSeeder userSeeder) {
        this.tenantCatalog = tenantCatalog;
        this.scenarioSeeder = scenarioSeeder;
        this.userSeeder = userSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String tenant : tenantCatalog.tenants()) {
            TenantContext.set(tenant);
            try {
                scenarioSeeder.seedIfEmpty();
                userSeeder.seedIfEmpty();
                log.info("Tenant onboarding complete: {}", tenant);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
