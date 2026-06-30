package com.payguard.infrastructure.tenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Aktif tenant'a göre doğru DataSource'u seçen yönlendirici.
 *
 * Spring her sorgudan önce determineCurrentLookupKey() çağırır; biz TenantContext'i döneriz.
 * null dönerse AbstractRoutingDataSource varsayılan (default) DataSource'u kullanır.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.get();
    }
}
