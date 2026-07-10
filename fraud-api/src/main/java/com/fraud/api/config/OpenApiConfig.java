package com.fraud.api.config;

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
 *
 * The bearer requirement set here applies GLOBALLY to every operation; login/refresh override it
 * with an empty @SecurityRequirements on the method (see AuthController) since those two are
 * public — without the override Swagger UI would wrongly demand a token before you can obtain one.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fraudOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud API")
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
