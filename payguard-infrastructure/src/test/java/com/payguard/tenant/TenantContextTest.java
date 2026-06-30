package com.payguard.tenant;

import com.payguard.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TenantContext'in null-safe davranışını kilitler — ScenarioCatalog'un cache anahtarı ve
 * TenantContextProvider bu davranışa (tenant set edilmemişse "default") güvenir.
 */
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void tenant_set_edilmemisse_default_doner() {
        assertEquals("default", TenantContext.currentOrDefault());
    }

    @Test
    void tenant_set_edilmisse_o_deger_doner() {
        TenantContext.set("alpha");
        assertEquals("alpha", TenantContext.currentOrDefault());
    }

    @Test
    void clear_sonrasi_tekrar_default_doner() {
        TenantContext.set("beta");
        TenantContext.clear();
        assertEquals("default", TenantContext.currentOrDefault());
    }
}
