package com.example.ocr.controller;

import com.example.ocr.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> home() {
        return ApiResponse.ok(Map.of(
                "service", "school-orc",
                "ocrApi", "/api/ocr",
                "swaggerUi", "/swagger-ui.html",
                "openApi", "/v3/api-docs",
                "health", "/actuator/health"
        ));
    }
}
