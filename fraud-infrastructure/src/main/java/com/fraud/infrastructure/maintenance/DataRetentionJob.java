package com.fraud.infrastructure.maintenance;

import com.fraud.infrastructure.persistence.MessageClaimJpaRepository;
import com.fraud.infrastructure.persistence.RefreshTokenJpaRepository;
import com.fraud.infrastructure.persistence.TransactionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Data retention for the tables that grow with every request (the outbox already has its own
 * cleanup in OutboxRelay):
 *
 * - transactions:    business records older than {@code fraud.retention.transactions-days} are
 *                    deleted. At real scale this would be archive-then-delete; plain deletion
 *                    keeps the demo scope honest while fixing the unbounded growth.
 * - message_claims:  dedup claims older than {@code fraud.retention.message-claims-days} are
 *                    deleted. CAREFUL TRADE-OFF: deleting a claim re-opens the duplicate-detection
 *                    window for that message id, so this retention must stay LONGER than any
 *                    realistic upstream retry window (default 30 days vs. retries measured in
 *                    minutes/hours).
 *
 * Both deletes are single bulk DELETE statements backed by the V5 indexes — no entities are
 * loaded into memory.
 */
@Component
public class DataRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionJob.class);

    private final TransactionJpaRepository transactions;
    private final MessageClaimJpaRepository claims;
    private final RefreshTokenJpaRepository refreshTokens;
    private final int transactionsDays;
    private final int messageClaimsDays;

    public DataRetentionJob(TransactionJpaRepository transactions,
                            MessageClaimJpaRepository claims,
                            RefreshTokenJpaRepository refreshTokens,
                            @Value("${fraud.retention.transactions-days:90}") int transactionsDays,
                            @Value("${fraud.retention.message-claims-days:30}") int messageClaimsDays) {
        this.transactions = transactions;
        this.claims = claims;
        this.refreshTokens = refreshTokens;
        this.transactionsDays = transactionsDays;
        this.messageClaimsDays = messageClaimsDays;
    }

    @Scheduled(fixedDelayString = "${fraud.retention.cleanup-interval-ms:86400000}")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int deletedTx = transactions.deleteByTransactionDateBefore(now.minus(transactionsDays, ChronoUnit.DAYS));
        int deletedClaims = claims.deleteByClaimedAtBefore(now.minus(messageClaimsDays, ChronoUnit.DAYS));
        // Expired refresh tokens are dead weight regardless of their used flag — no config knob
        // needed, the expiry itself IS the retention boundary.
        int deletedTokens = refreshTokens.deleteByExpiresAtBefore(now);
        if (deletedTx > 0 || deletedClaims > 0 || deletedTokens > 0) {
            log.info("Retention: deleted {} transactions (>{}d), {} message claims (>{}d), {} expired refresh tokens",
                    deletedTx, transactionsDays, deletedClaims, messageClaimsDays, deletedTokens);
        }
    }
}
