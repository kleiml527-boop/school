package com.example.ocr.config;

import com.example.ocr.core.engine.TesseractInstancePool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Tess4jConfig {

    @Bean(destroyMethod = "close")
    public TesseractInstancePool tesseractInstancePool(OcrProperties properties) {
        return new TesseractInstancePool(properties);
    }
}
