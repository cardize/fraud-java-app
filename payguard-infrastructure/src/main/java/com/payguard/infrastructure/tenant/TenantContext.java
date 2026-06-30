package com.payguard.infrastructure.tenant;

/**
 * O anki isteğin kiracısını (tenant) thread-local olarak tutar.
 *
 * Her HTTP isteği başında set edilir (TenantFilter), istek bitince temizlenir.
 * RoutingDataSource hangi DB'ye gidileceğini buradan okur.
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

    /** get()'in null-safe hali; tenant set edilmemişse (tek-kiracı modu) "default" döner. */
    public static String currentOrDefault() {
        String tenant = CURRENT.get();
        return tenant != null ? tenant : "default";
    }

    public static void clear() {
        CURRENT.remove();
    }
}
