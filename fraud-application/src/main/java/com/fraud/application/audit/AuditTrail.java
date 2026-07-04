package com.fraud.application.audit;

/**
 * PORT: persistent audit trail for security-relevant actions (logins, scenario mutations...).
 *
 * The application layer only states WHAT happened; WHO did it (the authenticated principal) and
 * the correlation id are resolved by the adapter at the composition root — the application layer
 * has no access to (and must not depend on) the security context.
 *
 * When called inside a handler's @Transactional block, the audit row commits atomically with the
 * mutation it describes — an audited action cannot exist without its audit record and vice versa.
 */
public interface AuditTrail {

    /**
     * @param action UPPER_SNAKE action code (e.g. SCENARIO_CREATED, LOGIN_FAILURE)
     * @param detail short human-readable context (ids, names); truncated by the adapter if long
     */
    void record(String action, String detail);
}
