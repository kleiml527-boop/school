package com.example.ocr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ocrOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot OCR API")
                        .version("v1")
                        .description("Pure Java OCR service powered by Tess4J"));
    }
}
