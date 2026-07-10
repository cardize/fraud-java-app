package com.fraud.outbox.publisher;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fraud.infrastructure.outbox.publisher.LoggingMessagePublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default, broker-free publisher (fraud.outbox.publisher=logging). Its entire contract IS
 * the log line, so the test captures it with a Logback ListAppender instead of mocking anything.
 */
class LoggingMessagePublisherTest {

    private final LoggingMessagePublisher publisher = new LoggingMessagePublisher();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(LoggingMessagePublisher.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void publishLogsDestinationAndPayload() {
        publisher.publish("fraud.offline-operations", "{\"fraudResponseCode\":\"APPROVE\"}");

        assertTrue(appender.list.stream().anyMatch(event ->
                event.getFormattedMessage().contains("fraud.offline-operations")
                        && event.getFormattedMessage().contains("APPROVE")));
    }
}
