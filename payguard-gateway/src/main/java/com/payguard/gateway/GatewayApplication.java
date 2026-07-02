package com.payguard.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway entry point.
 *
 * A thin reverse proxy that routes requests to the backing API (payguard-api).
 * Routes are defined in application.yml.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
