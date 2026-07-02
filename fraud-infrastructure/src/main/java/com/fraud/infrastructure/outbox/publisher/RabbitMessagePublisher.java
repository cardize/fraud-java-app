package com.fraud.infrastructure.outbox.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ publisher. Active when fraud.outbox.publisher=rabbit.
 *
 * RabbitTemplate is auto-configured by Spring Boot (spring.rabbitmq.*).
 * destination is used as the exchange/routing-key.
 */
@Component
@ConditionalOnProperty(name = "fraud.outbox.publisher", havingValue = "rabbit")
public class RabbitMessagePublisher implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String destination, String payload) {
        rabbitTemplate.convertAndSend(destination, payload);
    }
}
