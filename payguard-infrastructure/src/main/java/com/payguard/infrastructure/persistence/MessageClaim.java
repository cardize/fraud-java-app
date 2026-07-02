package com.payguard.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Atomic message-claim row: used for race-free duplicate detection.
 *
 * A SECOND insert attempt for the same (messageId, module) pair is rejected by the database's
 * UNIQUE constraint. This replaces the "SELECT to check first, then INSERT" pattern's exposure to
 * a race condition (TOCTOU) — the INSERT itself acts as an atomic mutex.
 */
@Entity
@Table(name = "message_claims", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "module"}))
public class MessageClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long messageId;
    private int module;

    protected MessageClaim() {
    }

    public MessageClaim(long messageId, int module) {
        this.messageId = messageId;
        this.module = module;
    }
}
