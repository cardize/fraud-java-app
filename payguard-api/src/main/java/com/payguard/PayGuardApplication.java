package com.payguard;

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
public class PayGuardApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayGuardApplication.class, args);
    }
}
