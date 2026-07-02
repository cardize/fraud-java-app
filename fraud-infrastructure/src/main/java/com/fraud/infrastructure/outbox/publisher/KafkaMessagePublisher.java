package com.fraud.infrastructure.outbox.publisher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka publisher. Active when fraud.outbox.publisher=kafka.
 *
 * KafkaTemplate is auto-configured by Spring Boot (spring.kafka.bootstrap-servers).
 * Outbox + Kafka together complete the "transactional outbox -> at-least-once delivery" pattern.
 */
@Component
@ConditionalOnProperty(name = "fraud.outbox.publisher", havingValue = "kafka")
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
