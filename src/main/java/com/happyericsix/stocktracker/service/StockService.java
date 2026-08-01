package com.happyericsix.stocktracker.service;

import com.happyericsix.stocktracker.client.AkshareStockClient;
import com.happyericsix.stocktracker.dto.*;
import com.happyericsix.stocktracker.entity.FavoriteStock;
import com.happyericsix.stocktracker.entity.User;
import com.happyericsix.stocktracker.exception.FavoriteAlreadyExistsException;
import com.happyericsix.stocktracker.repository.FavoriteStockRepository;
import com.happyericsix.stocktracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final AkshareStockClient akshareStockClient;
    private final FavoriteStockRepository favoriteStockRepository;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    @Autowired
    public StockService(AkshareStockClient akshareStockClient, FavoriteStockRepository favoriteStockRepository, UserRepository userRepository) {
        this.akshareStockClient = akshareStockClient;
        this.favoriteStockRepository = favoriteStockRepository;
        this.userRepository = userRepository;
    }

    // ==================== 股票搜索 ====================

    /**
     * 搜索股票代码/名称，永久缓存到 Redis。
     * 空关键词也缓存空结果，防止缓存穿透。
     */
    @Cacheable(value = "stockSearch", key = "#keyword.trim().toUpperCase()", unless = "#result == null")
    public StockSearchResponse searchStocks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new StockSearchResponse("", 0, Collections.emptyList());
        }
        return akshareStockClient.searchStocks(keyword.trim());
    }

    // ==================== 实时行情 ====================

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
                    .price(null)
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
    public FavoriteStock addFavorite(final String stockSymbol, final String username, final Double buyPrice, final Integer quantity) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (favoriteStockRepository.existsByStockSymbolAndUserId(stockSymbol, user.getId())) {
            throw new FavoriteAlreadyExistsException(stockSymbol);
        }

        FavoriteStock favoriteStock = FavoriteStock.builder()
                .stockSymbol(stockSymbol)
                .user(user)
                .buyPrice(buyPrice)
                .quantity(quantity)
                .buyDate(java.time.LocalDate.now().toString())
                .build();
        return favoriteStockRepository.save(favoriteStock);
    }

    @Transactional
    public boolean deleteFavorite(final String stockSymbol, final String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (favoriteStockRepository.existsByStockSymbolAndUserId(stockSymbol, user.getId())) {
            favoriteStockRepository.deleteByStockSymbolAndUserId(stockSymbol, user.getId());
            log.info("Deleted favorite: {} for user: {}", stockSymbol, username);
            return true;
        }
        log.warn("Favorite not found: {} for user: {}", stockSymbol, username);
        return false;
    }

    public List<StockResponse> getFavoritesWithLivePrices() {
        List<FavoriteStock> allFavorites = favoriteStockRepository.findAll();
        return allFavorites.stream()
                .map(fav -> getStockForSymbol(fav.getStockSymbol()))
                .collect(Collectors.toList());
    }

    public List<StockResponse> getFavoritesWithLivePrices(final String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        List<FavoriteStock> favoriteStocks = favoriteStockRepository.findByUserId(user.getId());
        return favoriteStocks.stream()
                .map(fav -> getStockForSymbol(fav.getStockSymbol()))
                .collect(Collectors.toList());
    }
}
