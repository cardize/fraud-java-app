package com.fraud.infrastructure.tenant;

import java.util.List;

/**
 * The known tenant ids (only produced as a bean in the 'multitenant' profile — see
 * MultiTenantDataSourceConfig).
 *
 * A dedicated type instead of a raw List&lt;String&gt; bean on purpose: Spring treats collection
 * injection points as "collect all beans of the element type", which would make a List&lt;String&gt;
 * dependency ambiguous; a unique wrapper type resolves cleanly.
 */
public record TenantCatalog(List<String> tenants) {
}
