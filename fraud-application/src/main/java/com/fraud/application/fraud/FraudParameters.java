package com.fraud.application.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Parameter set evaluated by the rule engine.
 *
 * Rules (SpEL expressions) access this object's getters.
 * Example rule expression: "amount > 5000 and hourOfDay < 6"
 */
public class FraudParameters {

    private final UUID transactionId;
    private final String shadowCardNo;
    private final BigDecimal amount;
    private final String merchantId;
    private final Instant transactionDate;

    // computed fields — for convenience in rule expressions
    private final int hourOfDay;
    private final double threshold;

    public FraudParameters(UUID transactionId, String shadowCardNo, BigDecimal amount,
                          String merchantId, Instant transactionDate, double threshold) {
        this.transactionId = transactionId;
        this.shadowCardNo = shadowCardNo;
        this.amount = amount;
        this.merchantId = merchantId;
        this.transactionDate = transactionDate;
        this.threshold = threshold;
        this.hourOfDay = transactionDate.atZone(ZoneOffset.UTC).getHour();
    }

    public UUID getTransactionId() { return transactionId; }
    public String getShadowCardNo() { return shadowCardNo; }
    public BigDecimal getAmount() { return amount; }
    /** So rule expressions can use it without a method call (safe SpEL). */
    public double getAmountValue() { return amount.doubleValue(); }
    public String getMerchantId() { return merchantId; }
    public Instant getTransactionDate() { return transactionDate; }
    public int getHourOfDay() { return hourOfDay; }
    public double getThreshold() { return threshold; }
}
