package com.payguard.infrastructure.tenant;

/**
 * O anki isteğin kiracısını (tenant) thread-local olarak tutar.
 *
 * .NET karşılığı: ITenantProvider (GetTenant). Her HTTP isteği başında set edilir (TenantFilter),
 * istek bitince temizlenir. RoutingDataSource hangi DB'ye gidileceğini buradan okur.
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

    public static void clear() {
        CURRENT.remove();
    }
}
