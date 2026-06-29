package com.payguard.infrastructure.outbox.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Varsayılan yayımcı: broker gerektirmez, sadece loglar (offline/öğrenme için).
 * payguard.outbox.publisher=logging (veya ayar yoksa) aktif.
 */
@Component
@ConditionalOnProperty(name = "payguard.outbox.publisher", havingValue = "logging", matchIfMissing = true)
public class LoggingMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessagePublisher.class);

    @Override
    public void publish(String destination, String payload) {
        log.info("[LOG-PUBLISH] {} <- {}", destination, payload);
    }
}
