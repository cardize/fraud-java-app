package com.payguard.tenant;

import com.payguard.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks in TenantContext's null-safe behavior — ScenarioCatalog's cache key and
 * TenantContextProvider both rely on this behavior (falling back to "default" when unset).
 */
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void returnsDefaultWhenTenantNotSet() {
        assertEquals("default", TenantContext.currentOrDefault());
    }

    @Test
    void returnsTheSetValueWhenTenantIsSet() {
        TenantContext.set("alpha");
        assertEquals("alpha", TenantContext.currentOrDefault());
    }

    @Test
    void returnsDefaultAgainAfterClear() {
        TenantContext.set("beta");
        TenantContext.clear();
        assertEquals("default", TenantContext.currentOrDefault());
    }
}
