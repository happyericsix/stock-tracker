package com.abhinav.stocktracker.service;

import com.abhinav.stocktracker.client.StockClient;
import com.abhinav.stocktracker.dto.*;
import com.abhinav.stocktracker.entity.FavoriteStock;
import com.abhinav.stocktracker.exception.FavoriteAlreadyExistsException;
import com.abhinav.stocktracker.repository.FavoriteStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StockService {

    private final StockClient stockClient;
    private final FavoriteStockRepository favoriteStockRepository;
    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    @Autowired
    public StockService(StockClient stockClient,  FavoriteStockRepository favoriteStockRepository) {
        this.stockClient = stockClient;
        this.favoriteStockRepository = favoriteStockRepository;
    }

    @Cacheable(value = "stocks", key = "#stockSymbol")
    public StockResponse getStockForSymbol(final String stockSymbol) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching stock quote for symbol: {} (cache miss)", stockSymbol);
        
        AlphaVantageResponse response = stockClient.getStockQuote(stockSymbol);
        if (response == null || response.globalQuote() == null) {
            // API did not return a valid quote (could be rate limit, invalid symbol, network issue)
            long duration = System.currentTimeMillis() - startTime;
            log.warn("No quote available for symbol {}. Rate limit or API error. Duration: {}ms", stockSymbol, duration);
            return StockResponse.builder()
                    .symbol(stockSymbol)
                    .price("0.0")
                    .lastUpdated(null)
                    .build();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Successfully fetched quote for {}. Duration: {}ms", stockSymbol, duration);
        return StockResponse.builder()
                .symbol(response.globalQuote().symbol())
                .price(response.globalQuote().price())
                .lastUpdated(response.globalQuote().lastTradingDay())
                .build();
    }

    @Cacheable(value = "stockOverviews", key = "#stockSymbol")
    public StockOverviewResponse getStockOverviewForSymbol(final String stockSymbol) {
        return stockClient.getStockOverview(stockSymbol);
    }

    public StockHistoryResponse getHistory(final String stockSymbol, int days) {
        return stockClient.getStockHistory(stockSymbol);
    }

    @Transactional
    public FavoriteStock addFavorite(final String stockSymbol) {
        if(favoriteStockRepository.existsByStockSymbol(stockSymbol)) {
            throw new FavoriteAlreadyExistsException(stockSymbol);
        }

        FavoriteStock favoriteStock = FavoriteStock.builder()
                .stockSymbol(stockSymbol)
                .build();

        return favoriteStockRepository.save(favoriteStock);
    }

    public List<StockResponse> getFavoritesWithLivePrices() {
        List<FavoriteStock> favoriteStocks = favoriteStockRepository.findAll();
        return favoriteStocks.stream()
                .map(fav -> getStockForSymbol(fav.getStockSymbol()))
                .collect(Collectors.toList());
    }
}
