package com.happyericsix.stocktracker.client;

import com.happyericsix.stocktracker.annotation.StockApiRetry;
import com.happyericsix.stocktracker.dto.AlphaVantageResponse;
import com.happyericsix.stocktracker.dto.StockHistoryResponse;
import com.happyericsix.stocktracker.dto.StockOverviewResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockClient {

    private final RestClient tencentRestClient;
    private final RestClient tencentHistoryRestClient;

    private static final Logger log = LoggerFactory.getLogger(StockClient.class);

    public StockClient(
            @Qualifier("tencentRestClient") RestClient tencentRestClient,
            @Qualifier("tencentHistoryRestClient") RestClient tencentHistoryRestClient) {
        this.tencentRestClient = tencentRestClient;
        this.tencentHistoryRestClient = tencentHistoryRestClient;
    }

    private String toTencentSymbol(String symbol) {
        String lower = symbol.toLowerCase();
        // 如果已带市场前缀，直接使用（大小写不敏感）
        if (lower.startsWith("sh") || lower.startsWith("sz") ||
            lower.startsWith("hk") || lower.startsWith("us")) {
            return lower;
        }
        // 自动判断：6开头→沪市，0/3开头→深市，其他→美股
        if (symbol.startsWith("6")) return "sh" + symbol;
        if (symbol.startsWith("0") || symbol.startsWith("3")) return "sz" + symbol;
        return "us" + symbol;
    }

    @StockApiRetry
    @CircuitBreaker(name = "stockQuote", fallbackMethod = "fallbackGetStockQuote")
    public AlphaVantageResponse getStockQuote(String symbol) {
        String tencentSym = toTencentSymbol(symbol);
        String raw = tencentRestClient.get()
                .uri("/q=" + tencentSym)
                .retrieve()
                .body(String.class);

        return parseQuote(raw, symbol);
    }

    private AlphaVantageResponse parseQuote(String raw, String symbol) {
        try {
            int start = raw.indexOf("\"") + 1;
            int end = raw.lastIndexOf("\"");
            if (start <= 0 || end <= start) {
                log.warn("Failed to find quotes in response for {}: start={}, end={}", symbol, start, end);
                return emptyQuote(symbol);
            }
            String[] fields = raw.substring(start, end).split("~");

            String price = fields.length > 3 ? fields[3] : "0.0";
            String date;

            // A股(带sh/sz前缀)：时间在fields[30]附近，但稳妥起见用当天
            // 美股(us前缀或无前缀)：时间在fields[43]
            String symbolLower = symbol.toLowerCase();
            if (symbolLower.startsWith("sh") || symbolLower.startsWith("sz") || symbolLower.startsWith("hk")) {
                // A股/港股：用当天日期，价格已是实时
                date = LocalDate.now().toString();
            } else {
                // 美股：fields[43] 一般是更新时间字符串
                date = fields.length > 43 ? fields[43] : LocalDate.now().toString();
            }

            return new AlphaVantageResponse(
                    new AlphaVantageResponse.GlobalQuote(symbol, price, date),
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to parse Tencent quote for {}: {}", symbol, e.getMessage());
            return emptyQuote(symbol);
        }
    }

    private AlphaVantageResponse emptyQuote(String symbol) {
        return new AlphaVantageResponse(
                new AlphaVantageResponse.GlobalQuote(symbol, "0.0", LocalDate.now().toString()),
                null
        );
    }

    @StockApiRetry
    @CircuitBreaker(name = "StockOverview", fallbackMethod = "fallbackgetStockOverview")
    public StockOverviewResponse getStockOverview(String symbol) {
        String tencentSym = toTencentSymbol(symbol);
        String raw = tencentRestClient.get()
                .uri("/q=" + tencentSym)
                .retrieve()
                .body(String.class);

        try {
            int start = raw.indexOf("\"") + 1;
            int end = raw.lastIndexOf("\"");
            if (start > 0 && end > start) {
                String[] fields = raw.substring(start, end).split("~");
                String name = fields.length > 1 ? fields[1] : symbol;
                String marketCap = "N/A";
                String peRatio = "N/A";
                // A股：fields[30]是日期，fields[31]是时间，不是市值和市盈率
                // 美股：fields[30]是市值，fields[31]是市盈率
                String symbolLower = symbol.toLowerCase();
                if (symbolLower.startsWith("sh") || symbolLower.startsWith("sz") || symbolLower.startsWith("hk")) {
                    // A股/港股市值市盈率需要其他接口，默认显示N/A
                } else {
                    marketCap = fields.length > 30 ? fields[30] : "N/A";
                    peRatio = fields.length > 31 ? fields[31] : "N/A";
                }
                return new StockOverviewResponse(
                        symbol, name, "", "N/A", "N/A",
                        marketCap, peRatio, "N/A"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse Tencent overview for {}: {}", symbol, e.getMessage());
        }

        return new StockOverviewResponse(symbol, symbol, "", "", "", "", "", "");
    }

    @StockApiRetry
    @CircuitBreaker(name = "StockHistory", fallbackMethod = "fallbackgetStockHistory")
    public StockHistoryResponse getStockHistory(String symbol) {
        String raw = tencentHistoryRestClient.get()
                .uri("/appstock/app/kline/kline?param=" + toTencentSymbol(symbol) + ",day,,,100")
                .retrieve()
                .body(String.class);

        return parseHistory(raw, symbol);
    }

    @SuppressWarnings("unchecked")
    private StockHistoryResponse parseHistory(String raw, String symbol) {
        try {
            tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
            Map<String, Object> root = mapper.readValue(raw, Map.class);
            Map<String, Object> data = (Map<String, Object>) root.get("data");
            if (data == null) return emptyHistory(symbol);

            Map<String, Object> stockData = (Map<String, Object>) data.get(toTencentSymbol(symbol));
            if (stockData == null) stockData = (Map<String, Object>) data.get(symbol);
            if (stockData == null) return emptyHistory(symbol);

            List<List<Object>> dayData = (List<List<Object>>) stockData.get("day");
            if (dayData == null || dayData.isEmpty()) return emptyHistory(symbol);

            Map<String, StockHistoryResponse.DailyPrice> timeSeries = new LinkedHashMap<>();
            for (List<Object> day : dayData) {
                if (day.size() < 6) continue;
                String date = String.valueOf(day.get(0));
                String open = String.valueOf(day.get(1));
                String close = String.valueOf(day.get(2));
                String high = String.valueOf(day.get(3));
                String low = String.valueOf(day.get(4));
                String volume = String.valueOf(day.get(5));
                timeSeries.put(date, new StockHistoryResponse.DailyPrice(open, high, low, close, volume));
            }

            return new StockHistoryResponse(
                    new StockHistoryResponse.MetaData(symbol),
                    timeSeries
            );
        } catch (Exception e) {
            log.warn("Failed to parse Tencent history for {}: {}", symbol, e.getMessage());
            return emptyHistory(symbol);
        }
    }

    private StockHistoryResponse emptyHistory(String symbol) {
        return new StockHistoryResponse(
                new StockHistoryResponse.MetaData(symbol),
                Map.of()
        );
    }

    @Recover
    public AlphaVantageResponse recoverGetStockQuote(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return emptyQuote(symbol);
    }

    @Recover
    public StockOverviewResponse recoverGetStockOverview(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return new StockOverviewResponse(symbol, symbol, "", "", "", "", "", "");
    }

    @Recover
    public StockHistoryResponse recoverGetStockHistory(Exception e, String symbol) {
        log.warn("All retries exhausted for symbol: {}", symbol);
        return emptyHistory(symbol);
    }

    private AlphaVantageResponse fallbackGetStockQuote(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        return emptyQuote(symbol);
    }

    private StockOverviewResponse fallbackgetStockOverview(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        return new StockOverviewResponse(symbol, symbol, "", "", "", "", "", "");
    }

    private StockHistoryResponse fallbackgetStockHistory(Throwable t, String symbol) {
        log.warn("Circuit breaker triggered for symbol: {}", symbol);
        return emptyHistory(symbol);
    }
}
