package com.payguard.infrastructure.outbox.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ yayımcısı. payguard.outbox.publisher=rabbit ile aktif.
 *
 * RabbitTemplate Spring Boot tarafından otomatik konfigüre edilir (spring.rabbitmq.*).
 * destination, exchange/routing-key olarak kullanılır.
 */
@Component
@ConditionalOnProperty(name = "payguard.outbox.publisher", havingValue = "rabbit")
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
