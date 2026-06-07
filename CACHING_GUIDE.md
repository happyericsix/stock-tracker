# Stock Tracker - Caching & Rate Limit Guide

## What Happened with Rate Limiting

You hit the **AlphaVantage API rate limit** (5 requests per minute on the free tier). After hitting this limit, the API returns empty quotes, and the app logs a warning and returns a placeholder response:
- `price: "0.0"`
- `lastUpdated: null`

This is **expected behavior** and prevents errors when the API is temporarily unavailable.

## How Caching Works

**Caching is now enabled** for faster responses:

### Cached Endpoints
1. **GET `/api/v1/stocks/{symbol}`** - Caches stock quotes by symbol
2. **GET `/api/v1/stocks/{symbol}/overview`** - Caches stock overviews by symbol

### Cache Key
Each cache entry is keyed by **stock symbol**. Example:
- First call to `GET /AAPL` → API call (slow, ~500ms-2s)
- Second call to `GET /AAPL` → Cache hit (fast, <5ms)
- First call to `GET GOOGL` → New cache entry (API call again)

### Cache Type
- **In-memory cache** (Spring's built-in ConcurrentHashMap)
- Caches available: `stocks`, `stockOverviews`
- Configured in `application.properties`

## Testing Cache Behavior

### Option 1: Use the Provided Test Script

```powershell
# Run this from your project root
.\test-cache.ps1
```

This will:
- Make 3 requests to the same symbol
- Show response times for each request
- Demonstrate cache speedup on calls 2-3

### Option 2: Manual Testing with Postman

1. **Start the app:**
   ```bash
   mvn spring-boot:run
   ```

2. **Make 3 GET requests to the same symbol:**
   ```
   GET http://localhost:8082/api/v1/stocks/AAPL
   ```

3. **Observe response times:**
   - Request 1: Slow (API call)
   - Requests 2-3: Fast (cache hit)

4. **Check server logs** for cache activity:
   ```
   [INFO] Fetching stock quote for symbol: AAPL (cache miss)
   [INFO] Successfully fetched quote for AAPL. Duration: 1234ms
   [INFO] GET /AAPL returned in 1234ms (price: 235.50)
   ```

   Next calls show:
   ```
   [INFO] GET /AAPL returned in 2ms (price: 235.50)
   ```

## Handling Rate Limits

### Currently
When you hit the rate limit (5 calls/min):
- The API returns no data
- Cache stores the placeholder response (0.0, null)
- Subsequent calls return the cached placeholder instantly

### Recommendation: Implement Exponential Backoff
To better handle rate limits in the future, consider:
1. Add Resilience4j `@Retry` and `@CircuitBreaker` annotations
2. Implement retry logic with exponential backoff
3. Cache responses longer when API is rate-limited
4. Return HTTP 429 (Too Many Requests) to clients

## Configuration

**Cache Settings** in `application.properties`:
```properties
spring.cache.type=simple
spring.cache.cache-names=stocks,stockOverviews
logging.level.org.springframework.cache=DEBUG
```

**To disable caching** (if needed):
- Remove `@EnableCaching` from `StocktrackerApplication.java`
- Set `spring.cache.type=none` in `application.properties`

**To enable external caching** (Redis, Memcached):
- Add dependency: `spring-boot-starter-data-redis`
- Change: `spring.cache.type=redis`
- Update properties with Redis connection details

## Improvement Plan

### Phase 1 (Current)
✅ Basic in-memory caching with @Cacheable

### Phase 2 (To Do)
- [ ] Add Retry logic with exponential backoff
- [ ] Circuit breaker pattern for API failures
- [ ] Cache eviction policies (TTL-based)
- [ ] Rate limit error response (HTTP 429)

### Phase 3 (Future)
- [ ] Switch to Redis for distributed caching
- [ ] Cache metrics/statistics endpoint
- [ ] Cache warming strategy for popular stocks

## API Endpoints

### Get Stock Quote (Cached)
```
GET /api/v1/stocks/{symbol}
Response: { symbol, price, lastUpdated }
```

### Get Stock Overview (Cached)
```
GET /api/v1/stocks/{symbol}/overview
Response: { company info, metrics, etc }
```

### Get Favorites with Prices (Uses Cache)
```
GET /api/v1/stocks/favorites
Response: List<{ symbol, price, lastUpdated }>
Note: Each favorite stock lookup uses the cache
```

### Add Favorite (Not Cached)
```
POST /api/v1/stocks/favorites
Body: { "symbol": "AAPL" }
Response: { id, stockSymbol }
```

## Troubleshooting

### Issue: Getting "0.0" price and null lastUpdated
**Cause:** Hit AlphaVantage rate limit (5/min free tier)
**Solution:** 
- Wait 1 minute before next request
- Consider upgrading to paid AlphaVantage plan for higher limits

### Issue: Not seeing cache speedup
**Check:**
1. Looking at same symbol (cache is per-symbol, not global)
2. Server logs for "cache miss" vs "cache hit" messages
3. Response time differences (should be 100x+ faster on cache hit)

### Issue: Cache not working
**Verify:**
- `@EnableCaching` is present on `StocktrackerApplication`
- `@Cacheable` annotations are on service methods
- `spring.cache.type=simple` is set in properties
- Rebuild/redeploy the app

## Example Test Output

```
✓ Call 1 (CACHE MISS): 1234ms - API call
✓ Call 2 (CACHE HIT):     2ms - Instant
✓ Call 3 (CACHE HIT):     1ms - Instant

Speedup: 600x faster on cached calls!
```

