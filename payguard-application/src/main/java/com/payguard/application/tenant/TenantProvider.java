package com.payguard.application.tenant;

/**
 * PORT for querying the current request's tenant.
 *
 * The application layer doesn't know the technical details of tenant resolution (thread-local,
 * header parsing...) — it only uses this interface. The implementation (adapter) lives in
 * INFRASTRUCTURE.
 */
public interface TenantProvider {

    /** The current request's tenant id; "default" in single-tenant mode / when no tenant is set. */
    String currentTenant();
}
