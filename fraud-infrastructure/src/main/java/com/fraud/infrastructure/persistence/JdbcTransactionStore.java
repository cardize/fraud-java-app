package com.fraud.infrastructure.persistence;

import com.fraud.application.transactions.TransactionStore;
import com.fraud.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * JdbcTemplate (raw SQL) ADAPTER for the TransactionStore port — the "hot path" implementation.
 *
 * Skips ORM overhead on the hot path (the record executed on every transaction) and uses direct
 * SQL: the "CRUD = JPA, hot path = JdbcTemplate" split.
 *
 * To activate: application.yml -> fraud.persistence.transaction-store: jdbc
 * (Only ONE TransactionStore bean is ever active at a time; @ConditionalOnProperty guarantees this.)
 *
 * NOTE: Column names follow the schema produced by Hibernate's default snake_case strategy
 * (messageId -> message_id). Pinned down by the schema migrations in production (Flyway/Liquibase).
 */
@Component
@ConditionalOnProperty(name = "fraud.persistence.transaction-store", havingValue = "jdbc")
public class JdbcTransactionStore implements TransactionStore {

    private final JdbcTemplate jdbc;

    public JdbcTransactionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** See {@link JpaTransactionStore#claimMessage} for the rationale behind REQUIRES_NEW. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimMessage(long messageId, int module) {
        try {
            jdbc.update("INSERT INTO message_claims (message_id, module) VALUES (?, ?)", messageId, module);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public void markPreviousAsNotLatest(long messageId, int module, UUID currentId) {
        jdbc.update(
                "UPDATE transactions SET latest_request = FALSE " +
                "WHERE message_id = ? AND module = ? AND id <> ?",
                messageId, module, currentId.toString());
    }

    @Override
    public void save(Transaction t) {
        // UUID PK, so a simple insert/merge instead of an upsert. We use MERGE on H2 (idempotent).
        jdbc.update(
                "MERGE INTO transactions " +
                "(id, message_id, module, shadow_card_no, amount, merchant_id, transaction_date, control_code, fraud_response_code, latest_request) " +
                "KEY(id) VALUES (?,?,?,?,?,?,?,?,?,?)",
                t.getId().toString(),
                t.getMessageId(),
                t.getModule(),
                t.getShadowCardNo(),
                t.getAmount(),
                t.getMerchantId(),
                t.getTransactionDate() != null ? Timestamp.from(t.getTransactionDate()) : null,
                t.getControlCode() != null ? t.getControlCode().name() : null,
                t.getFraudResponseCode(),
                t.isLatestRequest());
    }
}
