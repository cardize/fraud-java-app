package com.payguard.application.queue;

/**
 * Offline işlemleri yayımlama PORT'u.
 *
 * Application yalnızca bu arayüzü bilir; implementasyonu (Outbox/DB) INFRASTRUCTURE'dadır.
 * Handler bunu @Transactional içinde çağırır → mesaj, iş kaydıyla AYNI transaction'da yazılır
 * (atomik, transactional outbox). Böylece proses çökse bile mesaj kaybolmaz.
 */
public interface OfflineOperationPublisher {
    void publish(OfflineOperation operation);
}
