package com.payguard.infrastructure.tenant;

import com.payguard.application.tenant.TenantProvider;
import org.springframework.stereotype.Component;

/**
 * TenantProvider portunun ADAPTER'ı — geçerli kiracıyı TenantContext (thread-local) üzerinden okur.
 */
@Component
public class TenantContextProvider implements TenantProvider {

    @Override
    public String currentTenant() {
        return TenantContext.currentOrDefault();
    }
}
