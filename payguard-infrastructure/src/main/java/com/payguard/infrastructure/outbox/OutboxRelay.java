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
import java.util.List;

/**
 * Outbox relay: PENDING mesajları periyodik olarak işleyip yayımlar (PROCESSED yapar).
 *
 * Kaynak in-memory değil KALICI DB. Proses çökse bile PENDING kayıtlar DB'de durur ve yeniden
 * başlayınca işlenir. Yayım hedefi seçili MessagePublisher (logging/kafka/rabbit) ile en-az-bir-kez teslim.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxJpaRepository repository;
    private final MessagePublisher messagePublisher;
    private final String destination;

    public OutboxRelay(OutboxJpaRepository repository,
                       MessagePublisher messagePublisher,
                       @Value("${payguard.outbox.destination:payguard.offline-operations}") String destination) {
        this.repository = repository;
        this.messagePublisher = messagePublisher;
        this.destination = destination;
    }

    @Scheduled(fixedDelayString = "${payguard.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processPending() {
        List<OutboxMessage> batch =
                repository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxMessage msg : batch) {
            publish(msg);
            msg.markProcessed(Instant.now());
        }
        repository.saveAll(batch);
        log.info("Outbox: {} mesaj işlendi", batch.size());
    }

    private void publish(OutboxMessage msg) {
        // Seçili yayımcıya (logging/kafka/rabbit) gönder. Gerçek broker'a en-az-bir-kez teslim.
        String payload = String.format("{\"type\":\"%s\",\"transactionId\":\"%s\",\"module\":%d,\"fraudResponseCode\":\"%s\",\"tenant\":\"%s\"}",
                msg.getType(), msg.getTransactionId(), msg.getModule(), msg.getFraudResponseCode(), msg.getTenant());
        messagePublisher.publish(destination, payload);
    }
}
