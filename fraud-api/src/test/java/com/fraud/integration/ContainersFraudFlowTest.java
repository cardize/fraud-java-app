package com.fraud.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test against real infrastructure (Docker: Postgres + Kafka).
 *
 * PRODUCTION-LIKE components instead of H2/logging:
 *  - Postgres: Flyway migrations (flyway-database-postgresql) + JPA run on a real DB
 *  - Kafka: the outbox relay publishes to REAL Kafka; the test verifies with a consumer
 *
 * PREREQUISITE: Docker. Without Docker the test is skipped/fails (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ContainersFraudFlowTest {

    private static final String TOPIC = "fraud.offline-operations";

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        // DB -> Postgres container
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Outbox -> real Kafka
        registry.add("fraud.outbox.publisher", () -> "kafka");
        registry.add("fraud.outbox.poll-interval-ms", () -> "1000");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fraudFlowRunsOnPostgresAndPublishesToKafka() throws Exception {
        // 1) login
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"fraud123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // 2) high amount -> REJECT (scenarios must have been seeded into Postgres)
        mockMvc.perform(post("/api/v1/transactions/get-fraud-response-for-card")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"module\":1,\"transactionMessageId\":7777,\"shadowCardNo\":\"CARD123\","
                                + "\"amount\":6000,\"merchantId\":\"M1\",\"transactionDate\":\"2026-01-01T03:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fraudResponseCode").value("REJECT"));

        // 3) the outbox relay should have published to real Kafka -> verify with a consumer
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps())) {
            consumer.subscribe(List.of(TOPIC));
            AtomicInteger received = new AtomicInteger();
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> {
                    if (r.value() != null && r.value().contains("\"fraudResponseCode\":\"REJECT\"")) {
                        received.incrementAndGet();
                    }
                });
                assertTrue(received.get() > 0, "Expected a REJECT message on Kafka");
            });
        }
    }

    private Properties consumerProps() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "fraud-test");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return p;
    }
}
