package com.fraud.infrastructure.outbox;

import com.fraud.application.queue.OfflineOperation;
import com.fraud.application.queue.OfflineOperationPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * OUTBOX adapter for the OfflineOperationPublisher port.
 *
 * publish() only writes an outbox row to the DB. Since the handler calls this inside
 * @Transactional, the row commits in the same transaction as the business record (atomic, no
 * loss). Actual publishing (Kafka/RabbitMQ) is {@link OutboxRelay}'s responsibility — this solves
 * the "dual write" problem.
 */
@Component
public class OutboxOfflineOperationPublisher implements OfflineOperationPublisher {

    private final OutboxJpaRepository repository;

    public OutboxOfflineOperationPublisher(OutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void publish(OfflineOperation op) {
        repository.save(new OutboxMessage(
                "PROCESS_OFFLINE_SCENARIOS",
                op.transactionId(),
                op.module(),
                op.fraudResponseCode(),
                op.tenant(),
                Instant.now()));
    }
}
