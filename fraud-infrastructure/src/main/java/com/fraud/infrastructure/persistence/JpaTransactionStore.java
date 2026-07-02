package com.fraud.infrastructure.persistence;

import com.fraud.application.transactions.TransactionStore;
import com.fraud.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * JPA ADAPTER for the TransactionStore port (hexagonal architecture).
 *
 * Fulfills application's TransactionStore interface by delegating to the JPA repository in
 * INFRASTRUCTURE. Application never sees this class; it only knows the port (interface).
 *
 * DEFAULT adapter: fraud.persistence.transaction-store=jpa (or unset).
 */
@Component
@ConditionalOnProperty(name = "fraud.persistence.transaction-store", havingValue = "jpa", matchIfMissing = true)
public class JpaTransactionStore implements TransactionStore {

    private final TransactionJpaRepository jpaRepository;
    private final MessageClaimJpaRepository claimRepository;

    public JpaTransactionStore(TransactionJpaRepository jpaRepository, MessageClaimJpaRepository claimRepository) {
        this.jpaRepository = jpaRepository;
        this.claimRepository = claimRepository;
    }

    /**
     * REQUIRES_NEW: the claim attempt runs ISOLATED from the caller's (the handler's) transaction,
     * in its own transaction. Otherwise, a unique-constraint violation could make Hibernate mark
     * the caller's transaction "rollback-only", breaking the normal save() call the handler makes
     * right afterward. A failed claim only rolls back this small, separate transaction; the
     * handler's actual transaction stays healthy.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimMessage(long messageId, int module) {
        try {
            claimRepository.saveAndFlush(new MessageClaim(messageId, module));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public void markPreviousAsNotLatest(long messageId, int module, UUID currentId) {
        jpaRepository.markPreviousAsNotLatest(messageId, module, currentId);
    }

    @Override
    public void save(Transaction transaction) {
        jpaRepository.save(transaction);
    }
}
