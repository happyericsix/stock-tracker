package com.abhinav.stocktracker.client;

import com.abhinav.stocktracker.dto.StockHistoryResponse;
import com.abhinav.stocktracker.dto.StockOverviewResponse;
import com.abhinav.stocktracker.dto.StockQuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 通过 WebClient 调用 Python akshare 数据微服务的客户端。
 * 替换原有的 ChoiceStockClient（东方财富 DLL 接口）。
 *
 * Python 服务地址由配置属性 akshare.api.base-url 控制，
 * 默认 http://localhost:8000
 */
@Service
public class AkshareStockClient {

    private static final Logger log = LoggerFactory.getLogger(AkshareStockClient.class);

    private final WebClient webClient;

    public AkshareStockClient(@Value("${akshare.api.base-url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        log.info("AkshareStockClient initialized, base URL: {}", baseUrl);
    }

    // ==================== 实时行情 ====================

    public StockQuoteResponse getStockQuote(String symbol) {
        try {
            return webClient.get()
                    .uri("/api/v1/quote/{symbol}", symbol)
                    .retrieve()
                    .bodyToMono(StockQuoteResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("akshare quote error for {}: HTTP {} {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return emptyQuote(symbol);
        } catch (Exception e) {
            log.warn("akshare quote exception for {}: {}", symbol, e.getMessage());
            return emptyQuote(symbol);
        }
    }

    private StockQuoteResponse emptyQuote(String symbol) {
        return new StockQuoteResponse(
                new StockQuoteResponse.GlobalQuote(symbol, "0.0", java.time.LocalDate.now().toString()),
                null
        );
    }

    // ==================== 基本面概况 ====================

    public StockOverviewResponse getStockOverview(String symbol) {
        try {
            return webClient.get()
                    .uri("/api/v1/overview/{symbol}", symbol)
                    .retrieve()
                    .bodyToMono(StockOverviewResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("akshare overview error for {}: HTTP {} {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return emptyOverview(symbol);
        } catch (Exception e) {
            log.warn("akshare overview exception for {}: {}", symbol, e.getMessage());
            return emptyOverview(symbol);
        }
    }

    private StockOverviewResponse emptyOverview(String symbol) {
        return new StockOverviewResponse(symbol, symbol, "", "N/A", "N/A", "N/A", "N/A", "N/A");
    }

    // ==================== 历史 K 线 ====================

    public StockHistoryResponse getStockHistory(String symbol) {
        try {
            return webClient.get()
                    .uri("/api/v1/history/{symbol}", symbol)
                    .retrieve()
                    .bodyToMono(StockHistoryResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("akshare history error for {}: HTTP {} {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return emptyHistory(symbol);
        } catch (Exception e) {
            log.warn("akshare history exception for {}: {}", symbol, e.getMessage());
            return emptyHistory(symbol);
        }
    }

    private StockHistoryResponse emptyHistory(String symbol) {
        return new StockHistoryResponse(
                new StockHistoryResponse.MetaData(symbol),
                java.util.Map.of()
        );
    }
}
