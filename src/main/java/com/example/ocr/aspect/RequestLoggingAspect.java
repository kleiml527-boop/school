package com.example.ocr.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);

    @Around("within(com.example.ocr.controller..*)")
    public Object logControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long durationMillis = (System.nanoTime() - start) / 1_000_000;
            log.info("{} completed in {} ms", joinPoint.getSignature().toShortString(), durationMillis);
        }
    }
}
