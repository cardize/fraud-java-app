package com.payguard.infrastructure.outbox;

import com.payguard.application.queue.OfflineOperation;
import com.payguard.application.queue.OfflineOperationPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * OfflineOperationPublisher port'unun OUTBOX adapter'ı.
 *
 * publish() sadece DB'ye bir outbox satırı yazar. Handler bunu @Transactional içinde çağırdığından
 * satır, iş kaydıyla aynı transaction'da commit olur (atomik, kayıpsız). Gerçek yayım (Kafka/RabbitMQ)
 * sorumluluğu {@link OutboxRelay}'e aittir → "dual write" problemi çözülür.
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
