package com.payguard.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional Outbox table row.
 *
 * Written in the SAME database transaction as the business record (transaction) -> either both
 * are written or neither is (atomic). This makes "queued the message but the DB didn't commit" /
 * "the DB committed but the message was lost" inconsistencies impossible. A relay publishes
 * PENDING records afterward.
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;               // message type, e.g. "PROCESS_OFFLINE_SCENARIOS"
    private UUID transactionId;
    private int module;
    private String fraudResponseCode;
    private String tenant;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant createdAt;
    private Instant processedAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(String type, UUID transactionId, int module,
                         String fraudResponseCode, String tenant, Instant createdAt) {
        this.type = type;
        this.transactionId = transactionId;
        this.module = module;
        this.fraudResponseCode = fraudResponseCode;
        this.tenant = tenant;
        this.status = OutboxStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void markProcessed(Instant when) {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = when;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public UUID getTransactionId() { return transactionId; }
    public int getModule() { return module; }
    public String getFraudResponseCode() { return fraudResponseCode; }
    public String getTenant() { return tenant; }
    public OutboxStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
