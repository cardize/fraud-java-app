package com.payguard.infrastructure.outbox.publisher;

/**
 * PORT through which the outbox relay publishes a message to the outside world.
 *
 * Three implementations: logging (default, no broker), kafka, rabbit. Which one is active is
 * decided by the payguard.outbox.publisher setting (@ConditionalOnProperty) — only one becomes a bean.
 */
public interface MessagePublisher {
    void publish(String destination, String payload);
}
