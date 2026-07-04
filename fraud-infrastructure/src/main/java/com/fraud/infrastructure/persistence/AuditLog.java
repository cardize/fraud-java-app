package com.fraud.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persistent audit record (who did what, when, under which correlation id).
 *
 * Append-only by design: no setters, no update path — an audit trail that can be edited
 * afterwards is not an audit trail.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant occurredAt;
    private String username;
    private String action;
    private String detail;
    private String correlationId;

    protected AuditLog() {
        // no-arg constructor required by JPA
    }

    public AuditLog(String username, String action, String detail, String correlationId) {
        this.occurredAt = Instant.now();
        this.username = username;
        this.action = action;
        this.detail = detail;
        this.correlationId = correlationId;
    }

    public Instant getOccurredAt() { return occurredAt; }
    public String getUsername() { return username; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public String getCorrelationId() { return correlationId; }
}
