package com.payguard.application.tenant;

/**
 * O anki isteğin kiracısını (tenant) sorgulama PORT'u.
 *
 * Application katmanı kiracı çözümlemesinin teknik detayını (thread-local, header okuma...)
 * bilmez; yalnızca bu arayüzü kullanır. İmplementasyon (adapter) INFRASTRUCTURE'dadır.
 */
public interface TenantProvider {

    /** Geçerli isteğin kiracı kimliği; tek-kiracı modunda/tenant set edilmemişse "default". */
    String currentTenant();
}
