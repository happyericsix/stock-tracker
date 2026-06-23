package com.abhinav.stocktracker.client;

import com.abhinav.stocktracker.annotation.StockApiRetry;
import com.abhinav.stocktracker.dto.AlphaVantageResponse;
import com.abhinav.stocktracker.dto.DailyStockResponse;
import com.abhinav.stocktracker.dto.StockHistoryResponse;
import com.abhinav.stocktracker.dto.StockOverviewResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
   @StockApiRetry
   @CircuitBreaker(name = "stockQuote", fallbackMethod = "fallbackGetStockQuote")
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

    @StockApiRetry
    @CircuitBreaker(name = "StockOverview", fallbackMethod = "fallbackgetStockOverview")
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
    @StockApiRetry
    @CircuitBreaker(name = "StockHistory", fallbackMethod = "fallbackgetStockHistory")
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
    @Recover
    public AlphaVantageResponse recoverGetStockQuote(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return null;
    }
    @Recover
    public StockOverviewResponse recoverGetStockOverview(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return null;
    }
    @Recover
    public StockHistoryResponse recoverGetStockHistory(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return null;
    }
    private AlphaVantageResponse fallbackGetStockQuote(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        if (t instanceof CallNotPermittedException) {
            log.warn("熔断器已打开，直接降级");
        } else {
            log.error("API调用异常，重试已耗尽", t);
        }
        return null;
    }
    private StockOverviewResponse fallbackgetStockOverview(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        if (t instanceof CallNotPermittedException) {
            log.warn("熔断器已打开，直接降级");
        } else {
            log.error("API调用异常，重试已耗尽", t);
        }
        return null;
    }
    private StockHistoryResponse fallbackgetStockHistory(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        if (t instanceof CallNotPermittedException) {
            log.warn("熔断器已打开，直接降级");
        } else {
            log.error("API调用异常，重试已耗尽", t);
        }
        return null;
    }
}
