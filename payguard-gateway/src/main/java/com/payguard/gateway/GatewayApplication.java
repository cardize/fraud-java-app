package com.payguard.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway giriş noktası.
 *
 * İstekleri arka taraftaki API'ye (payguard-api) yönlendiren ince bir reverse proxy.
 * Route'lar application.yml'de tanımlı.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
