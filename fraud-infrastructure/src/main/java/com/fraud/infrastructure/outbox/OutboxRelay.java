package com.fraud.infrastructure.outbox;

import com.fraud.infrastructure.outbox.publisher.MessagePublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.atomic.AtomicLong;

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

    // Observability: backlog gauge + publish outcome counters. A growing fraud.outbox.pending is
    // the earliest signal that the broker is down or the relay can't keep up — alert on it.
    private final AtomicLong pendingGauge;
    private final Counter publishedOk;
    private final Counter publishedError;

    public OutboxRelay(OutboxJpaRepository repository,
                       MessagePublisher messagePublisher,
                       MeterRegistry meterRegistry,
                       @Value("${fraud.outbox.destination:fraud.offline-operations}") String destination,
                       @Value("${fraud.outbox.retention-days:7}") int retentionDays) {
        this.repository = repository;
        this.messagePublisher = messagePublisher;
        this.destination = destination;
        this.retentionDays = retentionDays;
        this.pendingGauge = meterRegistry.gauge("fraud.outbox.pending", new AtomicLong());
        this.publishedOk = meterRegistry.counter("fraud.outbox.published", "result", "ok");
        this.publishedError = meterRegistry.counter("fraud.outbox.published", "result", "error");
    }

    @Scheduled(fixedDelayString = "${fraud.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processPending() {
        // MULTI-INSTANCE SAFETY lives on the repository method, not here: findByStatus... carries
        // @Lock(PESSIMISTIC_WRITE) + lock.timeout=-2, i.e. SELECT ... FOR UPDATE SKIP LOCKED on
        // Postgres. A second relay instance skips the rows this transaction holds and cannot
        // double-publish them. (Kept as an explicit note because a reviewer reading only this
        // class flagged the lock as missing.)
        List<OutboxMessage> batch =
                repository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(BATCH_SIZE));
        if (batch.isEmpty()) {
            pendingGauge.set(0);
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
                publishedOk.increment();
            } catch (Exception e) {
                publishedError.increment();
                log.error("Failed to publish outbox message (id={}), will retry on the next tick", msg.getId(), e);
            }
        }
        if (!processed.isEmpty()) {
            repository.saveAll(processed);
            log.info("Outbox: processed {}/{} messages", processed.size(), batch.size());
        }
        pendingGauge.set(repository.countByStatus(OutboxStatus.PENDING));
    }

    /**
     * Cleans up old PROCESSED records — prevents the outbox table from growing unbounded.
     * Runs once a day (much less frequently than the relay loop; this is purely maintenance/retention work).
     */
    @Scheduled(fixedDelayString = "${fraud.outbox.cleanup-interval-ms:86400000}")
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
