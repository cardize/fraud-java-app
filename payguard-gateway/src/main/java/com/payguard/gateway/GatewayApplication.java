package com.payguard.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway giriş noktası.
 *
 * .NET karşılığı: PayGuard.External.API/Program.cs (Ocelot host'u).
 * İstekleri arka taraftaki Internal API'ye (payguard-api) yönlendiren ince bir reverse proxy.
 * Route'lar application.yml'de tanımlı (ocelot.json karşılığı).
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
