package com.payguard.infrastructure.tenant;

/**
 * Holds the current request's tenant as a thread-local.
 *
 * Set at the start of every HTTP request (TenantFilter), cleared when the request ends.
 * RoutingDataSource reads this to decide which DB to use.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenant) {
        CURRENT.set(tenant);
    }

    public static String get() {
        return CURRENT.get();
    }

    /** Null-safe version of get(); returns "default" when no tenant is set (single-tenant mode). */
    public static String currentOrDefault() {
        String tenant = CURRENT.get();
        return tenant != null ? tenant : "default";
    }

    public static void clear() {
        CURRENT.remove();
    }
}
