package com.happyericsix.stocktracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置，为 AkshareStockClient 提供基础 Builder。
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder akshareWebClientBuilder() {
        return WebClient.builder();
    }
}
