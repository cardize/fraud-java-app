package com.payguard.infrastructure.outbox.publisher;

/**
 * Outbox relay'in mesajı dış dünyaya yayımladığı PORT.
 *
 * Üç implementasyon: logging (varsayılan, broker'sız), kafka, rabbit. Hangisinin aktif olacağı
 * payguard.outbox.publisher ayarıyla belirlenir (@ConditionalOnProperty) — yalnızca biri bean olur.
 */
public interface MessagePublisher {
    void publish(String destination, String payload);
}
