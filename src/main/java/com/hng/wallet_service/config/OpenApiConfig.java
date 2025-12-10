package com.hng.wallet_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI walletServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Wallet Service API")
                                                .description("HNG Stage 9 - Wallet Service with Paystack, JWT & API Keys")
                                                .version("1.0.0")
                                                )
                                .addSecurityItem(new SecurityRequirement()
                                                .addList("Bearer Authentication")
                                                .addList("API Key Authentication"))
                                .components(new Components()
                                                .addSecuritySchemes("Bearer Authentication",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("Enter JWT token"))
                                                .addSecuritySchemes("API Key Authentication",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.APIKEY)
                                                                                .in(SecurityScheme.In.HEADER)
                                                                                .name("x-api-key")
                                                                                .description("Enter API key")));
        }

        @Bean
        public GroupedOpenApi publicApi() {
                return GroupedOpenApi.builder()
                                .group("wallet-service")
                                .pathsToMatch("/**")
                                .build();
        }
}
