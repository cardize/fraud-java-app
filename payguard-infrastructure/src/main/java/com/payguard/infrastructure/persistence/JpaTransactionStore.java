package com.payguard.infrastructure.persistence;

import com.payguard.application.transactions.TransactionStore;
import com.payguard.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * TransactionStore portunun JPA ADAPTER'ı (hexagonal architecture).
 *
 * Application'daki TransactionStore arayüzünü, INFRASTRUCTURE'daki JPA repository'ye
 * delege ederek gerçekler. Application bu sınıfı görmez; sadece portu (interface) bilir.
 *
 * VARSAYILAN adapter: payguard.persistence.transaction-store=jpa (veya ayar yoksa).
 */
@Component
@ConditionalOnProperty(name = "payguard.persistence.transaction-store", havingValue = "jpa", matchIfMissing = true)
public class JpaTransactionStore implements TransactionStore {

    private final TransactionJpaRepository jpaRepository;
    private final MessageClaimJpaRepository claimRepository;

    public JpaTransactionStore(TransactionJpaRepository jpaRepository, MessageClaimJpaRepository claimRepository) {
        this.jpaRepository = jpaRepository;
        this.claimRepository = claimRepository;
    }

    /**
     * REQUIRES_NEW: claim denemesi çağıranın (handler'ın) transaction'ından İZOLE, kendi
     * transaction'ında çalışır. Aksi halde bir unique-constraint ihlali sonrası Hibernate
     * çağıranın transaction'ını "rollback-only" işaretleyebilir ve handler'ın hemen ardından
     * yapacağı normal save() çağrısı da bozulurdu. Başarısız claim yalnızca bu küçük, ayrı
     * transaction'ı geri alır; handler'ın asıl transaction'ı sağlıklı kalır.
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
