package com.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * - @SpringBootApplication: component scan + auto-configuration + DI container
 * - @EnableScheduling: so the outbox relay can run periodically
 */
@SpringBootApplication
@EnableScheduling
public class FraudApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudApplication.class, args);
    }
}
