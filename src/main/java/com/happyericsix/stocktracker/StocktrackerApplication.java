package com.happyericsix.stocktracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableRetry
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class StocktrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StocktrackerApplication.class, args);
    }

}
