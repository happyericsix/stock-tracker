package com.abhinav.stocktracker.service;

import com.abhinav.stocktracker.client.AkshareStockClient;
import com.abhinav.stocktracker.dto.*;
import com.abhinav.stocktracker.entity.FavoriteStock;
import com.abhinav.stocktracker.exception.FavoriteAlreadyExistsException;
import com.abhinav.stocktracker.repository.FavoriteStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final AkshareStockClient akshareStockClient;
    private final FavoriteStockRepository favoriteStockRepository;
    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    @Autowired
    public StockService(AkshareStockClient akshareStockClient, FavoriteStockRepository favoriteStockRepository) {
        this.akshareStockClient = akshareStockClient;
        this.favoriteStockRepository = favoriteStockRepository;
    }

    @Cacheable(value = "stocks", key = "#stockSymbol")
    public StockResponse getStockForSymbol(final String stockSymbol) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching stock quote for symbol: {} (cache miss)", stockSymbol);

        StockQuoteResponse response = akshareStockClient.getStockQuote(stockSymbol);

        if (response == null || response.globalQuote() == null) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("No quote data for symbol {} (empty response). Duration: {}ms", stockSymbol, duration);
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
        return akshareStockClient.getStockOverview(stockSymbol);
    }

    public StockHistoryResponse getHistory(final String stockSymbol, int days) {
        return akshareStockClient.getStockHistory(stockSymbol);
    }

    public PagedResponse<DailyStockResponse> getHistoryPaged(String symbol, int page, int size) {
        StockHistoryResponse response = akshareStockClient.getStockHistory(symbol);

        List<DailyStockResponse> allData = response.timeSeries().entrySet().stream()
                .map(entry -> new DailyStockResponse(
                        entry.getKey(),
                        Double.parseDouble(entry.getValue().open()),
                        Double.parseDouble(entry.getValue().close()),
                        Double.parseDouble(entry.getValue().high()),
                        Double.parseDouble(entry.getValue().low()),
                        Long.parseLong(entry.getValue().volume())
                ))
                .collect(Collectors.toList());

        int totalElements = allData.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<DailyStockResponse> pageContent = fromIndex >= totalElements
                ? List.of()
                : allData.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageContent, page, size, totalElements, totalPages);
    }

    @Transactional
    public FavoriteStock addFavorite(final String stockSymbol) {
        if (favoriteStockRepository.existsByStockSymbol(stockSymbol)) {
            throw new FavoriteAlreadyExistsException(stockSymbol);
        }
        FavoriteStock favoriteStock = FavoriteStock.builder()
                .stockSymbol(stockSymbol)
                .build();
        return favoriteStockRepository.save(favoriteStock);
    }

    @Transactional
    public boolean deleteFavorite(final String stockSymbol) {
        if (favoriteStockRepository.existsByStockSymbol(stockSymbol)) {
            favoriteStockRepository.deleteByStockSymbol(stockSymbol);
            log.info("Deleted favorite: {}", stockSymbol);
            return true;
        }
        log.warn("Favorite not found: {}", stockSymbol);
        return false;
    }

    public List<StockResponse> getFavoritesWithLivePrices() {
        List<FavoriteStock> favoriteStocks = favoriteStockRepository.findAll();
        return favoriteStocks.stream()
                .map(fav -> getStockForSymbol(fav.getStockSymbol()))
                .collect(Collectors.toList());
    }
}
