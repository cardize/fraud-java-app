package com.payguard.application.transactions;

import com.payguard.domain.transaction.Transaction;

import java.util.UUID;

/**
 * İşlem kalıcılık PORT'u (hexagonal architecture).
 *
 * Bu arayüz APPLICATION katmanında durur; implementasyonu (adapter) INFRASTRUCTURE'dadır.
 * Böylece application, JPA/Spring Data gibi altyapı detaylarına BAĞIMLI OLMAZ — bağımlılık
 * yönü her zaman içe doğrudur (api → infrastructure → application → domain).
 */
public interface TransactionStore {

    /**
     * Bir (messageId, module) çiftini ATOMİK olarak "ilk kez görülüyor" diye claim etmeye çalışır.
     *
     * @return true  — bu çağrı gerçekten ilk claim (NORMAL işlenmeli)
     *         false — daha önce claim edilmiş (DUPLICATE — fraud kontrolü atlanmalı)
     *
     * BUG DÜZELTMESİ: Önceki "önce var mı diye SELECT'le bak, sonra INSERT et" deseni race-free
     * DEĞİLDİ — aynı messageId ile eşzamanlı iki istek (örn. ağ retry'ı) ikisi de "yok" görüp ikisi
     * de NORMAL işlenebiliyordu (fraud senaryosu iki kez koşar, iki kez outbox'a yazılırdı).
     * Artık INSERT'in kendisi (UNIQUE constraint ihlali = "zaten var") atomik mutex görevi görür.
     */
    boolean claimMessage(long messageId, int module);

    void markPreviousAsNotLatest(long messageId, int module, UUID currentId);

    void save(Transaction transaction);
}
