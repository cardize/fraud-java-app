package com.fraud.outbox.publisher;

import com.fraud.infrastructure.outbox.publisher.RabbitMessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test with a mocked RabbitTemplate — no broker needed (this project has no RabbitMQ
 * Testcontainers coverage; Kafka is the only broker verified end to end, see
 * ContainersFraudFlowTest). This test locks in the adapter's contract: destination is used as
 * both the exchange and routing key argument of convertAndSend(destination, payload).
 */
class RabbitMessagePublisherTest {

    @Test
    void publishSendsDestinationAndPayloadToRabbitTemplate() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitMessagePublisher publisher = new RabbitMessagePublisher(rabbitTemplate);

        publisher.publish("fraud.offline-operations", "{\"fraudResponseCode\":\"REVIEW\"}");

        verify(rabbitTemplate).convertAndSend("fraud.offline-operations", "{\"fraudResponseCode\":\"REVIEW\"}");
    }
}
