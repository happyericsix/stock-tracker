package com.happyericsix.stocktracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebClientConfig {

    @Bean
    public RestClient tencentRestClient(@Value("${tencent.stock.base.url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    public RestClient tencentHistoryRestClient(@Value("${tencent.stock.history.base.url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}