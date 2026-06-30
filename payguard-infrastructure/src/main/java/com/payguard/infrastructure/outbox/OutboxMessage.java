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
 * Transactional Outbox tablosu satırı.
 *
 * İş kaydı (transaction) ile AYNI veritabanı transaction'ında yazılır → ya ikisi de yazılır
 * ya hiçbiri (atomik). Böylece "mesajı kuyruğa attım ama DB commit olmadı" / "DB commit oldu ama
 * mesaj kayboldu" tutarsızlıkları imkânsızlaşır. Bir relay sonradan PENDING kayıtları yayımlar.
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;               // mesaj tipi, örn "PROCESS_OFFLINE_SCENARIOS"
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
