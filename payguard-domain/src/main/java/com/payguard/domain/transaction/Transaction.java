package com.payguard.domain.transaction;

import com.payguard.domain.shared.ControlCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent transaction record (JPA entity). Mapping is done via JPA annotations (@Entity, @Id...).
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    private UUID id;

    private long messageId;
    private int module;
    private String shadowCardNo;
    private BigDecimal amount;
    private String merchantId;
    private Instant transactionDate;

    @Enumerated(EnumType.STRING)
    private ControlCode controlCode;

    private String fraudResponseCode;
    private boolean latestRequest;

    protected Transaction() {
        // no-arg constructor required by JPA
    }

    public Transaction(UUID id, long messageId, int module, String shadowCardNo,
                       BigDecimal amount, String merchantId, Instant transactionDate,
                       ControlCode controlCode) {
        this.id = id;
        this.messageId = messageId;
        this.module = module;
        this.shadowCardNo = shadowCardNo;
        this.amount = amount;
        this.merchantId = merchantId;
        this.transactionDate = transactionDate;
        this.controlCode = controlCode;
        this.latestRequest = true;
    }

    public UUID getId() { return id; }
    public long getMessageId() { return messageId; }
    public int getModule() { return module; }
    public String getShadowCardNo() { return shadowCardNo; }
    public BigDecimal getAmount() { return amount; }
    public String getMerchantId() { return merchantId; }
    public Instant getTransactionDate() { return transactionDate; }
    public ControlCode getControlCode() { return controlCode; }
    public String getFraudResponseCode() { return fraudResponseCode; }
    public boolean isLatestRequest() { return latestRequest; }

    public void setFraudResponseCode(String fraudResponseCode) {
        this.fraudResponseCode = fraudResponseCode;
    }
}
