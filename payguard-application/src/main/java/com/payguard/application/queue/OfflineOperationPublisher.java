package com.payguard.application.queue;

/**
 * Offline işlemleri yayımlama PORT'u.
 *
 * Application yalnızca bu arayüzü bilir; implementasyonu (Outbox/DB) INFRASTRUCTURE'dadır.
 * Handler bunu @Transactional içinde çağırır → mesaj, iş kaydıyla AYNI transaction'da yazılır
 * (atomik). Böylece .NET'teki in-memory kuyruğun "proses çökerse mesaj kaybolur" riski ortadan kalkar.
 *
 * .NET karşılığı: EnqueueOfflineOperations + Mq/Queue altyapısı — ama kalıcı (transactional outbox).
 */
public interface OfflineOperationPublisher {
    void publish(OfflineOperation operation);
}
