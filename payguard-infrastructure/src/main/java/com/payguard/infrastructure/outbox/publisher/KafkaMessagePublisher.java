package com.payguard.infrastructure.outbox.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka yayımcısı. payguard.outbox.publisher=kafka ile aktif.
 *
 * KafkaTemplate Spring Boot tarafından otomatik konfigüre edilir (spring.kafka.bootstrap-servers).
 * Outbox + Kafka birlikte "transactional outbox -> en-az-bir-kez teslim" desenini tamamlar.
 */
@Component
@ConditionalOnProperty(name = "payguard.outbox.publisher", havingValue = "kafka")
public class KafkaMessagePublisher implements MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaMessagePublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String destination, String payload) {
        kafkaTemplate.send(destination, payload);
    }
}
