package com.payguard.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 *
 * Swagger UI: http://localhost:8080/swagger-ui.html  ·  API schema: /v3/api-docs
 * Use the "Authorize" button to enter a Bearer token and try the protected endpoints.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI payGuardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayGuard API")
                        .description("Fraud detection platform")
                        .version("0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
