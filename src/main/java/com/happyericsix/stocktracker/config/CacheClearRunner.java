package com.happyericsix.stocktracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheClearRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheClearRunner.class);
    private final CacheManager cacheManager;

    public CacheClearRunner(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void run(String... args) {
        log.info("Clearing all caches on startup...");
        for (String name : cacheManager.getCacheNames()) {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                log.info("Cleared cache: {}", name);
            }
        }
    }
}