package com.abhinav.stocktracker.controller;

import com.abhinav.stocktracker.client.StockClient;
import com.abhinav.stocktracker.dto.*;
import com.abhinav.stocktracker.entity.FavoriteStock;
import com.abhinav.stocktracker.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;
    private final StockClient stockClient;
    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    @Autowired
    public StockController(StockService stockService, StockClient stockClient) {
        this.stockService = stockService;
        this.stockClient = stockClient;
    }


    @GetMapping("/{stockSymbol}")
    public StockResponse getStock(@PathVariable("stockSymbol") String stockSymbol) {
        long startTime = System.currentTimeMillis();
        StockResponse response = stockService.getStockForSymbol(stockSymbol.toUpperCase());
        long duration = System.currentTimeMillis() - startTime;
        log.info("GET /{} returned in {}ms (price: {})", stockSymbol.toUpperCase(), duration, response.price());
        return response;
    }

    @GetMapping("/{stockSymbol}/overview")
    public StockOverviewResponse getStockOverview(@PathVariable String stockSymbol) {
        long startTime = System.currentTimeMillis();
        StockOverviewResponse response = stockService.getStockOverviewForSymbol(stockSymbol.toUpperCase());
        long duration = System.currentTimeMillis() - startTime;
        log.info("GET /{}/overview returned in {}ms", stockSymbol.toUpperCase(), duration);
        return response;
    }

    @GetMapping("/{stockSymbol}/history")
    public List<DailyStockResponse> getStockHistory(@PathVariable String stockSymbol, @RequestParam(defaultValue = "30") int days) {
        StockHistoryResponse response = stockClient.getStockHistory(stockSymbol.toUpperCase());
        return response.timeSeries().entrySet().stream()
                .limit(days)
                .map(entry -> {
                    var date = entry.getKey();
                    var daily = entry.getValue();
                    return new DailyStockResponse(
                            date,
                            Double.parseDouble(daily.open()),
                            Double.parseDouble(daily.close()),
                            Double.parseDouble(daily.high()),
                            Double.parseDouble(daily.low()),
                            Long.parseLong(daily.volume())
                    );
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/favorites")
    public ResponseEntity<FavoriteStock> saveFavoriteStock(@RequestBody FavoriteStockRequest request) {
        final FavoriteStock saved = stockService.addFavorite(request.getSymbol());
        return  ResponseEntity.ok().body(saved);
    }

    @GetMapping("/favorites")
    public List<StockResponse> getFavoriteStocks() {
        long startTime = System.currentTimeMillis();
        List<StockResponse> favorites = stockService.getFavoritesWithLivePrices();
        long duration = System.currentTimeMillis() - startTime;
        log.info("GET /favorites returned {} stocks in {}ms", favorites.size(), duration);
        return favorites;
    }
}
