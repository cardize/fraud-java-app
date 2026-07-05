package com.fraud.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The claim INSERT in its own REQUIRES_NEW transaction — deliberately a SEPARATE bean.
 *
 * WHY REQUIRES_NEW: the claim attempt must be isolated from the caller's (the handler's)
 * transaction, so a unique-constraint violation only rolls back this small transaction and the
 * handler's transaction stays healthy for the duplicate path's save().
 *
 * WHY A SEPARATE BEAN (the subtle part — found by DuplicateClaimConcurrencyTest on a real run):
 * the try/catch must sit OUTSIDE this transaction boundary. The first version caught the
 * violation INSIDE the same REQUIRES_NEW method — but the exception had already passed through
 * Spring Data's own repository @Transactional interceptor (a participant of the inner
 * transaction), which marked it rollback-only. Catching after that is too late: the method
 * returned normally, its interceptor tried to COMMIT a rollback-only transaction, and threw
 * UnexpectedRollbackException into the handler — every duplicate became an HTTP 500 instead of a
 * graceful DUPLICATE. With the boundary isolated here and the catch in JpaTransactionStore
 * (outside the proxy call), a failed insert rolls back cleanly and the caller just sees the
 * exception.
 */
@Component
public class MessageClaimInserter {

    private final MessageClaimJpaRepository repository;

    public MessageClaimInserter(MessageClaimJpaRepository repository) {
        this.repository = repository;
    }

    /** @throws org.springframework.dao.DataIntegrityViolationException when the claim already exists */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(long messageId, int module) {
        repository.saveAndFlush(new MessageClaim(messageId, module));
    }
}
