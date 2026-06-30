package com.payguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Uygulama giriş noktası.
 *
 * - @SpringBootApplication: component-scan + auto-configuration + DI konteyneri
 * - @EnableScheduling: Outbox relay'in periyodik çalışması için
 */
@SpringBootApplication
@EnableScheduling
public class PayGuardApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayGuardApplication.class, args);
    }
}
