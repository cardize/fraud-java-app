package com.payguard.infrastructure.outbox.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default publisher: requires no broker, just logs (for offline/learning use).
 * Active when payguard.outbox.publisher=logging (or unset).
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
