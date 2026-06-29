package com.payguard.infrastructure.persistence;

import com.payguard.application.transactions.TransactionStore;
import com.payguard.domain.transaction.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TransactionStore portunun JPA ADAPTER'ı (hexagonal architecture).
 *
 * Application'daki TransactionStore arayüzünü, INFRASTRUCTURE'daki JPA repository'ye
 * delege ederek gerçekler. Application bu sınıfı görmez; sadece portu (interface) bilir.
 *
 * VARSAYILAN adapter: payguard.persistence.transaction-store=jpa (veya ayar yoksa).
 * .NET karşılığı: yönetim/CRUD yolundaki EF Core repository.
 */
@Component
@ConditionalOnProperty(name = "payguard.persistence.transaction-store", havingValue = "jpa", matchIfMissing = true)
public class JpaTransactionStore implements TransactionStore {

    private final TransactionJpaRepository jpaRepository;

    public JpaTransactionStore(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByMessageIdAndModule(long messageId, int module) {
        return jpaRepository.existsByMessageIdAndModule(messageId, module);
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
