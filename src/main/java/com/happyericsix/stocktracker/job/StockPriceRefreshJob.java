package com.happyericsix.stocktracker.job;

import com.happyericsix.stocktracker.service.StockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StockPriceRefreshJob {

    private final StockService stockService;

    public StockPriceRefreshJob(StockService stockService) {
        this.stockService = stockService;
    }
    // 每 5 分钟执行一次（配合 cache 30s 过期，用户访问时少量回源也算合理）
    @Scheduled(fixedRate = 300_000)
    public void refreshFavoritePrices() {
        stockService.getFavoritesWithLivePrices();
    }
}