package com.fraud.infrastructure.persistence;

import com.fraud.application.transactions.TransactionStore;
import com.fraud.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

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
    private final MessageClaimInserter claimInserter;

    public JpaTransactionStore(TransactionJpaRepository jpaRepository, MessageClaimInserter claimInserter) {
        this.jpaRepository = jpaRepository;
        this.claimInserter = claimInserter;
    }

    /**
     * The INSERT runs in its own REQUIRES_NEW transaction inside {@link MessageClaimInserter};
     * the catch sits HERE, OUTSIDE that transaction boundary. Catching inside the boundary was a
     * real bug: Spring Data's repository interceptor had already marked the inner transaction
     * rollback-only, so the "successful" return then blew up at commit with
     * UnexpectedRollbackException (see MessageClaimInserter's javadoc for the full story).
     */
    @Override
    public boolean claimMessage(long messageId, int module) {
        try {
            claimInserter.insert(messageId, module);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // another request already holds the claim -> DUPLICATE
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
