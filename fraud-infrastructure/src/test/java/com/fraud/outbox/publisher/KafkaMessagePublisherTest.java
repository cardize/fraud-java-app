package com.fraud.outbox.publisher;

import com.fraud.infrastructure.outbox.publisher.KafkaMessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test with a mocked KafkaTemplate — no broker needed. The real Kafka wiring (does the
 * outbox relay actually reach a broker end to end) is covered separately by
 * ContainersFraudFlowTest (Testcontainers). This test only locks in the adapter's own contract:
 * publish() forwards destination/payload to KafkaTemplate.send() unchanged.
 */
class KafkaMessagePublisherTest {

    @Test
    void publishSendsDestinationAndPayloadToKafkaTemplate() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaMessagePublisher publisher = new KafkaMessagePublisher(kafkaTemplate);

        publisher.publish("fraud.offline-operations", "{\"fraudResponseCode\":\"REJECT\"}");

        verify(kafkaTemplate).send("fraud.offline-operations", "{\"fraudResponseCode\":\"REJECT\"}");
    }
}
