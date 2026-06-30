package com.payguard.infrastructure.persistence;

import com.payguard.application.transactions.TransactionStore;
import com.payguard.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * TransactionStore portunun JdbcTemplate (ham SQL) ADAPTER'ı — "hot path" implementasyonu.
 *
 * Sıcak yolda (her işlemde çalışan kayıt) ORM yükünü atlayıp doğrudan SQL kullanır:
 * "CRUD = JPA, sıcak yol = JdbcTemplate" ayrımı.
 *
 * Aktive etmek için: application.yml -> payguard.persistence.transaction-store: jdbc
 * (Aynı anda yalnızca BİR TransactionStore bean'i aktif olur; @ConditionalOnProperty bunu sağlar.)
 *
 * NOT: Kolon adları Hibernate'in varsayılan snake_case stratejisiyle oluşan şemaya göredir
 * (messageId -> message_id). Üretimde şema migration ile sabitlenir (Flyway/Liquibase).
 */
@Component
@ConditionalOnProperty(name = "payguard.persistence.transaction-store", havingValue = "jdbc")
public class JdbcTransactionStore implements TransactionStore {

    private final JdbcTemplate jdbc;

    public JdbcTransactionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** REQUIRES_NEW gerekçesi için bkz. {@link JpaTransactionStore#claimMessage}. */
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
        // UUID PK olduğundan upsert yerine basit insert/merge. H2'de MERGE kullanıyoruz (idempotent).
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
