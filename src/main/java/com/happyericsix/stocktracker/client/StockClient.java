package com.abhinav.stocktracker.client;

import com.abhinav.stocktracker.dto.AlphaVantageResponse;
import com.abhinav.stocktracker.dto.DailyStockResponse;
import com.abhinav.stocktracker.dto.StockHistoryResponse;
import com.abhinav.stocktracker.dto.StockOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockClient {

    private final WebClient webClient;

    // inject API key and base url
    @Value("${alpha.vantage.api.key}")
    private String apiKey;

    @Value("${alpha.vantage.base.url}")
    private String baseUrl;

    private static final Logger log = LoggerFactory.getLogger(StockClient.class);

    public AlphaVantageResponse getStockQuote(String symbol) {
            return webClient.get()
                    .uri(builder -> builder
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(AlphaVantageResponse.class)
                    .block();
    }


    public StockOverviewResponse  getStockOverview(final String symbol) {
        return webClient.get()
                .uri(builder -> builder
                        .queryParam("function", "OVERVIEW")
                        .queryParam("symbol", symbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(StockOverviewResponse.class)
                .block();
    }

    public StockHistoryResponse getStockHistory(String stockSymbol) {
        return webClient.get()
                .uri(builder -> builder
                        .queryParam("function", "TIME_SERIES_DAILY")
                        .queryParam("symbol", stockSymbol)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(StockHistoryResponse.class)
                .block();
    }
}
