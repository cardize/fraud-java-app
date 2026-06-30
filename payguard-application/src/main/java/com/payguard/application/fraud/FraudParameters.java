package com.payguard.application.fraud;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Kural motorunun değerlendireceği parametre seti.
 *
 * Kurallar (SpEL ifadeleri) bu nesnenin getter'larına erişir.
 * Örn kural ifadesi: "amount > 5000 and hourOfDay < 6"
 */
public class FraudParameters {

    private final UUID transactionId;
    private final String shadowCardNo;
    private final BigDecimal amount;
    private final String merchantId;
    private final Instant transactionDate;

    // hesaplanmış (computed) alanlar — kurallarda kullanım kolaylığı için
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
    /** Kural ifadelerinin metot çağrısı yapmadan (güvenli SpEL) kullanabilmesi için. */
    public double getAmountValue() { return amount.doubleValue(); }
    public String getMerchantId() { return merchantId; }
    public Instant getTransactionDate() { return transactionDate; }
    public int getHourOfDay() { return hourOfDay; }
    public double getThreshold() { return threshold; }
}
