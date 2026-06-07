package com.abhinav.stocktracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class StocktrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StocktrackerApplication.class, args);
    }

}
