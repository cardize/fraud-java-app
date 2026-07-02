package com.payguard.infrastructure.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Router that picks the right DataSource based on the active tenant.
 *
 * Spring calls determineCurrentLookupKey() before every query; we return TenantContext.
 * If it returns null, AbstractRoutingDataSource falls back to the default DataSource.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.get();
    }
}
