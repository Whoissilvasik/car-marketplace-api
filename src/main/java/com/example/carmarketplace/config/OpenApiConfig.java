package com.example.carmarketplace.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carMarketplaceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Car Marketplace API")
                .description("REST API for managing car sale listings")
                .version("1.0.0"));
    }
}
