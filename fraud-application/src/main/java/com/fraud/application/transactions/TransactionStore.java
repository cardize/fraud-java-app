package com.fraud.application.transactions;

import com.fraud.domain.transaction.Transaction;

import java.util.UUID;

/**
 * Transaction persistence PORT (hexagonal architecture).
 *
 * This interface lives in the APPLICATION layer; its implementation (adapter) lives in
 * INFRASTRUCTURE. This keeps application from DEPENDING on infrastructure details like JPA/Spring
 * Data — the dependency direction always points inward (api → infrastructure → application → domain).
 */
public interface TransactionStore {

    /**
     * Attempts to ATOMICALLY claim a (messageId, module) pair as "seen for the first time".
     *
     * @return true  — this call is genuinely the first claim (should be processed as NORMAL)
     *         false — already claimed before (DUPLICATE — the fraud check should be skipped)
     *
     * BUGFIX: The previous "SELECT to check if it exists, then INSERT" pattern was NOT race-free —
     * two concurrent requests with the same messageId (e.g. a network retry) could both see "not
     * found" and both be processed as NORMAL (the fraud scenario would run twice and be written to
     * the outbox twice). Now the INSERT itself (a UNIQUE constraint violation meaning "already
     * exists") acts as an atomic mutex.
     */
    boolean claimMessage(long messageId, int module);

    void markPreviousAsNotLatest(long messageId, int module, UUID currentId);

    void save(Transaction transaction);
}
