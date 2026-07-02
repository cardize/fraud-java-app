package com.payguard.application.queue;

/**
 * Offline-operation publishing PORT.
 *
 * Application only knows this interface; the implementation (outbox/DB) lives in INFRASTRUCTURE.
 * The handler calls this inside @Transactional -> the message is written in the SAME transaction
 * as the business record (atomic, transactional outbox). So the message can't be lost even if the
 * process crashes.
 */
public interface OfflineOperationPublisher {
    void publish(OfflineOperation operation);
}
