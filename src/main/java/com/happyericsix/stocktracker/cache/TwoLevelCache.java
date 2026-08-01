package com.happyericsix.stocktracker.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final Cache<Object, Object> caffeineCache;
    private final org.springframework.cache.Cache redisCache;

    public TwoLevelCache(String name, Cache<Object, Object> caffeineCache,
                         org.springframework.cache.Cache redisCache, boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.caffeineCache = caffeineCache;
        this.redisCache = redisCache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    private Object caffeineKey(Object key) {
        return name + "::" + key;
    }

    @Override
    @Nullable
    protected Object lookup(Object key) {
        Object caffeineKey = caffeineKey(key);
        Object value = caffeineCache.getIfPresent(caffeineKey);
        if (value != null) {
            return value;
        }

        ValueWrapper wrapper = redisCache.get(key);
        if (wrapper != null) {
            value = wrapper.get();
            caffeineCache.put(caffeineKey, value);
            return value;
        }

        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) fromStoreValue(value);
        }

        try {
            T loaded = valueLoader.call();
            put(key, toStoreValue(loaded));
            return loaded;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        Object storeValue = toStoreValue(value);
        caffeineCache.put(caffeineKey(key), storeValue);
        redisCache.put(key, storeValue);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        Object storeValue = toStoreValue(value);
        ValueWrapper existing = redisCache.putIfAbsent(key, storeValue);
        if (existing == null) {
            caffeineCache.put(caffeineKey(key), storeValue);
            return null;
        }
        caffeineCache.put(caffeineKey(key), existing.get());
        return existing;
    }

    @Override
    public void evict(Object key) {
        caffeineCache.invalidate(caffeineKey(key));
        redisCache.evict(key);
    }

    @Override
    public void clear() {
        caffeineCache.invalidateAll();
        redisCache.clear();
    }
}