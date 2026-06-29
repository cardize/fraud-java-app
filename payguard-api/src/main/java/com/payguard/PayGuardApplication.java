package com.payguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Uygulama giriş noktası.
 *
 * .NET karşılığı: PayGuard.Internal.API/Program.cs + Startup.cs
 * - @SpringBootApplication: component-scan + auto-configuration + DI konteyneri (Autofac karşılığı)
 * - @EnableScheduling: Outbox relay'in periyodik çalışması için (.NET BackgroundService karşılığı)
 */
@SpringBootApplication
@EnableScheduling
public class PayGuardApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayGuardApplication.class, args);
    }
}
