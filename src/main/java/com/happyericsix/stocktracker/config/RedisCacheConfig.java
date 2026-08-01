package com.happyericsix.stocktracker.config;

import com.happyericsix.stocktracker.cache.TwoLevelCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Set;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig {

    public static final Set<String> CACHE_NAMES = Set.of("stocks", "stockOverviews");

    @Bean
    public com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(25))
                .maximumSize(500)
                .recordStats()
                .build();
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new JdkSerializationRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .initialCacheNames(CACHE_NAMES)
                .build();
    }

    @Bean
    @Primary
    public CacheManager twoLevelCacheManager(
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache,
            RedisCacheManager redisCacheManager) {

        return new CacheManager() {
            @Override
            public org.springframework.cache.Cache getCache(String name) {
                org.springframework.cache.Cache redisCache = redisCacheManager.getCache(name);
                if (redisCache == null) return null;
                return new TwoLevelCache(name, caffeineCache, redisCache, false);
            }

            @Override
            public java.util.Collection<String> getCacheNames() {
                return redisCacheManager.getCacheNames();
            }
        };
    }
}