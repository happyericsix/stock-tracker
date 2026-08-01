package com.happyericsix.stocktracker.annotation;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retryable(
        retryFor = Exception.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StockApiRetry {
}