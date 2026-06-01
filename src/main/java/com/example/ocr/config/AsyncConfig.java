package com.example.ocr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "ocrTaskExecutor")
    public Executor ocrTaskExecutor(OcrProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getPoolSize());
        executor.setMaxPoolSize(properties.getPoolSize() * 2);
        executor.setQueueCapacity(properties.getPoolSize() * 20);
        executor.setThreadNamePrefix("ocr-");
        executor.initialize();
        return executor;
    }
}
