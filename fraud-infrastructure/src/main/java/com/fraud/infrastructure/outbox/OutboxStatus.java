package com.fraud.infrastructure.outbox;

/**
 * Outbox message status.
 * PENDING: written, not yet published. PROCESSED: published by the relay.
 */
public enum OutboxStatus {
    PENDING,
    PROCESSED
}
