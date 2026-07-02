package com.payguard.infrastructure.tenant;

import com.payguard.application.tenant.TenantProvider;
import org.springframework.stereotype.Component;

/**
 * ADAPTER for the TenantProvider port — reads the current tenant via TenantContext (thread-local).
 */
@Component
public class TenantContextProvider implements TenantProvider {

    @Override
    public String currentTenant() {
        return TenantContext.currentOrDefault();
    }
}
