package com.payguard.infrastructure.outbox;

import com.payguard.infrastructure.outbox.publisher.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Outbox relay: periodically processes and publishes PENDING messages (marking them PROCESSED).
 *
 * The source is a PERSISTENT DB, not in-memory. Even if the process crashes, PENDING records stay
 * in the DB and get processed on restart. Publishing goes through whichever MessagePublisher is
 * selected (logging/kafka/rabbit), with at-least-once delivery.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxJpaRepository repository;
    private final MessagePublisher messagePublisher;
    private final String destination;
    private final int retentionDays;

    public OutboxRelay(OutboxJpaRepository repository,
                       MessagePublisher messagePublisher,
                       @Value("${payguard.outbox.destination:payguard.offline-operations}") String destination,
                       @Value("${payguard.outbox.retention-days:7}") int retentionDays) {
        this.repository = repository;
        this.messagePublisher = messagePublisher;
        this.destination = destination;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${payguard.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processPending() {
        List<OutboxMessage> batch =
                repository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }
        // Per-message isolation: if one fails the others still get processed; the failed one
        // stays PENDING and is retried on the next tick (a poison message can't block the queue).
        List<OutboxMessage> processed = new ArrayList<>();
        for (OutboxMessage msg : batch) {
            try {
                publish(msg);
                msg.markProcessed(Instant.now());
                processed.add(msg);
            } catch (Exception e) {
                log.error("Failed to publish outbox message (id={}), will retry on the next tick", msg.getId(), e);
            }
        }
        if (!processed.isEmpty()) {
            repository.saveAll(processed);
            log.info("Outbox: processed {}/{} messages", processed.size(), batch.size());
        }
    }

    /**
     * Cleans up old PROCESSED records — prevents the outbox table from growing unbounded.
     * Runs once a day (much less frequently than the relay loop; this is purely maintenance/retention work).
     */
    @Scheduled(fixedDelayString = "${payguard.outbox.cleanup-interval-ms:86400000}")
    @Transactional
    public void cleanupProcessed() {
        Instant cutoff = Instant.now().minusSeconds(retentionDays * 86_400L);
        int deleted = repository.deleteProcessedBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox: cleaned up {} old PROCESSED records (retention={} days)", deleted, retentionDays);
        }
    }

    private void publish(OutboxMessage msg) {
        // Send to the selected publisher (logging/kafka/rabbit). At-least-once delivery to the real broker.
        String payload = String.format("{\"type\":\"%s\",\"transactionId\":\"%s\",\"module\":%d,\"fraudResponseCode\":\"%s\",\"tenant\":\"%s\"}",
                msg.getType(), msg.getTransactionId(), msg.getModule(), msg.getFraudResponseCode(), msg.getTenant());
        messagePublisher.publish(destination, payload);
    }
}
