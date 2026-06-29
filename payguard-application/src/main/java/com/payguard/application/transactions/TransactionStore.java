package com.payguard.application.transactions;

import com.payguard.domain.transaction.Transaction;

import java.util.UUID;

/**
 * İşlem kalıcılık PORT'u (hexagonal architecture).
 *
 * Bu arayüz APPLICATION katmanında durur; implementasyonu (adapter) INFRASTRUCTURE'dadır.
 * Böylece application, JPA/Spring Data gibi altyapı detaylarına BAĞIMLI OLMAZ — bağımlılık
 * yönü her zaman içe doğrudur (api → infrastructure → application → domain).
 *
 * .NET karşılığı: handler'ın bağımlı olduğu IRuleDapperRepository arayüzü
 * (interface Application'da, Dapper implementasyonu Infrastructure'da).
 */
public interface TransactionStore {

    boolean existsByMessageIdAndModule(long messageId, int module);

    void markPreviousAsNotLatest(long messageId, int module, UUID currentId);

    void save(Transaction transaction);
}
