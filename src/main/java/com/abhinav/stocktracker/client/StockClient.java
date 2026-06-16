package com.abhinav.stocktracker.client;

import com.abhinav.stocktracker.dto.AlphaVantageResponse;
import com.abhinav.stocktracker.dto.DailyStockResponse;
import com.abhinav.stocktracker.dto.StockHistoryResponse;
import com.abhinav.stocktracker.dto.StockOverviewResponse;
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
    @Retryable(
            retryFor = Exception.class,    // 遇到任何异常都重试
            maxAttempts = 3,               // 最多重试 3 次
            backoff = @Backoff(delay = 2000)  // 每次重试间隔 2 秒
    )
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

    @Retryable(
            retryFor = Exception.class,    // 遇到任何异常都重试
            maxAttempts = 3,               // 最多重试 3 次
            backoff = @Backoff(delay = 2000)  // 每次重试间隔 2 秒
    )
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
    @Retryable(
            retryFor = Exception.class,    // 遇到任何异常都重试
            maxAttempts = 3,               // 最多重试 3 次
            backoff = @Backoff(delay = 2000)  // 每次重试间隔 2 秒
    )
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
        return null;  // 返回 null，Service 层会处理成 price: "0.0"
    }
    @Recover
    public StockOverviewResponse getStockOverview(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return null;  // 返回 null，Service 层会处理成 price: "0.0"
    }
    @Recover
    public StockHistoryResponse getStockHistory(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return null;  // 返回 null，Service 层会处理成 price: "0.0"
    }
}
