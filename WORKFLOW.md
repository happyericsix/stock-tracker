# Stock Tracker - Request to Response Workflow

Complete layer-wise and flow diagrams showing how requests travel through the application from Postman to the final JSON response.

---

## 1. LAYER-WISE ARCHITECTURE

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                                 │
│                         (Postman / REST Client)                              │
│                      GET /api/v1/stocks/GOOGL                                │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │
                                   │ HTTP Request
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                           API LAYER                                          │
│                    (Spring Boot Controller)                                  │
│                                                                      
│  @RestController                                                             │
│  @RequestMapping("/api/v1/stocks")                                           │
│  StockController                                                             │
│  ├── @GetMapping("/{stockSymbol}")                                           │
│  ├── @GetMapping("/{stockSymbol}/overview")                                  │
│  ├── @GetMapping("/{stockSymbol}/history")                                   │
│  ├── @PostMapping("/favorites")                                              │
│  └── @GetMapping("/favorites")                                               │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │
                                   │ Calls Methods
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         BUSINESS LOGIC LAYER                                 │
│                      (Spring Service - StockService)                         │
│                                                                              │
│  @Service                                                                    │
│  ├── @Cacheable("stocks")                                                    │
│  │   getStockForSymbol(String)                                               │
│  │                                                                            │
│  ├── @Cacheable("stockOverviews")                                            │
│  │   getStockOverviewForSymbol(String)                                       │
│  │                                                                            │
│  ├── getHistory(String, int)                                                 │
│  │                                                                            │
│  ├── @Transactional                                                          │
│  │   addFavorite(String)                                                     │
│  │                                                                            │
│  └── getFavoritesWithLivePrices()                                            │
└───────┬─────────────────────────────────────┬───────────────────────────────┘
        │                                     │
        │ Cache Check                         │ No Cache Hit
        ▼                                     ▼
    ┌─────────────────┐            ┌──────────────────────────┐
    │   CACHE HIT     │            │  CLIENT LAYER            │
    │ (Return From    │            │  (StockClient)           │
    │ In-Memory Cache)│            │                          │
    └────────┬────────┘            │ ├─ getStockQuote()       │
             │                     │ ├─ getStockOverview()    │
             │                     │ └─ getStockHistory()     │
             │                     │                          │
             │                     │ (Uses WebClient/HTTP)    │
             │                     └────────┬────────┬────────┘
             │                              │        │
             │                              │        └──────────────┐
             │                              │                       │
             │                              ▼                       ▼
             │                    ┌──────────────────┐   ┌──────────────────┐
             │                    │   DATABASE       │   │  EXTERNAL API    │
             │                    │  (H2 In-Memory)  │   │ (Alpha Vantage)  │
             │                    │                  │   │                  │
             │                    │ FavoriteStock    │   │ GLOBAL_QUOTE     │
             │                    │ Entity           │   │ OVERVIEW         │
             │                    │                  │   │ TIME_SERIES_DAILY│
             │                    └────────┬─────────┘   └────────┬─────────┘
             │                             │                     │
             │                             └─────┬───────────────┘
             │                                   │
             │                    Response (DTO/JSON)
             │                                   │
             └───────────────────┬───────────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │    CACHE LAYER           │
                    │  (Spring Cache - Simple) │
                    │                          │
                    │ Cache Name: "stocks"     │
                    │ Cache Name: "stockOver"  │
                    └────────┬─────────────────┘
                             │
                             │ Store/Return
                             ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        RESPONSE LAYER (DTOs)                                 │
│                                                                              │
│  ├── StockResponse                                                           │
│  │   { symbol, price, lastUpdated }                                          │
│  │                                                                            │
│  ├── StockOverviewResponse                                                   │
│  │   { symbol, name, description, marketCap, peRatio, ... }                 │
│  │                                                                            │
│  ├── StockHistoryResponse                                                    │
│  │   [ { date, open, close, high, low, volume }, ... ]                      │
│  │                                                                            │
│  └── FavoriteStock (Entity)                                                  │
│      { id, stockSymbol }                                                     │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │
                                   │ JSON Serialization
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER (Response)                           │
│                         (Postman / REST Client)                              │
│                                                                              │
│  HTTP 200 OK                                                                 │
│  Content-Type: application/json                                              │
│                                                                              │
│  {                                                                           │
│    "symbol": "GOOGL",                                                        │
│    "price": "178.45",                                                        │
│    "lastUpdated": "2026-06-07"                                               │
│  }                                                                           │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. REQUEST-RESPONSE FLOW DIAGRAMS

### A. Get Stock Quote - `GET /api/v1/stocks/{stockSymbol}`

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ POSTMAN REQUEST                                                             │
│ ────────────────                                                            │
│ Method: GET                                                                 │
│ URL: http://localhost:8082/api/v1/stocks/GOOGL                              │
└────────────────────────┬────────────────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────┐
        │  StockController                   │
        │  @GetMapping("/{stockSymbol}")     │
        │                                    │
        │  1. Receive: stockSymbol = "GOOGL" │
        │  2. Convert to uppercase: "GOOGL"  │
        │  3. Call stockService              │
        │     .getStockForSymbol("GOOGL")    │
        │  4. Record start time              │
        │  5. Get response                   │
        └────────────────┬────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────────────┐
        │  StockService                            │
        │  @Cacheable(value="stocks",key="GOOGL")  │
        │                                          │
        │  Check Cache: "stocks" cache             │
        │        ↓                                  │
        │  ╔════════════════════╗                  │
        │  ║  CACHE HIT?        ║                  │
        │  ╚═════╤══════════════╝                  │
        │        │                                 │
        │   Yes  │   No                            │
        │    ↓   └──────┐                          │
        │    │          ▼                          │
        │    │  ┌────────────────────────┐         │
        │    │  │  StockClient           │         │
        │    │  │                        │         │
        │    │  │ WebClient.get()        │         │
        │    │  │  └─ function:          │         │
        │    │  │    GLOBAL_QUOTE        │         │
        │    │  │  └─ symbol: GOOGL      │         │
        │    │  │  └─ apikey: xxxxx      │         │
        │    │  │                        │         │
        │    │  │ HTTP Call to:          │         │
        │    │  │ https://www.alphavantage.co/    │
        │    │  └────────┬───────────────┘         │
        │    │           │                         │
        │    │           ▼                         │
        │    │  AlphaVantageResponse               │
        │    │  {                                  │
        │    │    "Global Quote": {                │
        │    │      "01. symbol": "GOOGL",         │
        │    │      "05. price": "178.45",         │
        │    │      "07. latest trading day"...    │
        │    │    }                                │
        │    │  }                                  │
        │    │           │                         │
        │    └───────────┼────────┐                │
        │                │        │                │
        │                ▼        ▼                │
        │        ┌─────────────────────────┐      │
        │        │ Parse & Build Response  │      │
        │        │ StockResponse object    │      │
        │        │ {                       │      │
        │        │  symbol: "GOOGL",       │      │
        │        │  price: "178.45",       │      │
        │        │  lastUpdated: "2026-..." │     │
        │        │ }                       │      │
        │        └──────────┬──────────────┘      │
        │                   │                     │
        │                   ▼                     │
        │        ┌─────────────────────────┐      │
        │        │  Cache the Result       │      │
        │        │  (In-Memory Cache)      │      │
        │        │  Key: "GOOGL"           │      │
        │        └──────────┬──────────────┘      │
        │                   │                     │
        └───────────────────┼─────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │  StockResponse JSON                   │
        │  ──────────────────────────────────── │
        │  {                                    │
        │    "symbol": "GOOGL",                 │
        │    "price": "178.45",                 │
        │    "lastUpdated": "2026-06-07"        │
        │  }                                    │
        └───────────────┬───────────────────────┘
                        │
                        ▼
        ┌──────────────────────────────────────┐
        │  StockController                     │
        │                                      │
        │  6. Calculate request duration       │
        │  7. Log: "GET /GOOGL returned in     │
        │          XXms (price: 178.45)"       │
        │  8. Return response to Postman       │
        └──────────────┬───────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │ POSTMAN RESPONSE                     │
        │ ────────────────────                 │
        │ Status: 200 OK                       │
        │ Content-Type: application/json       │
        │                                      │
        │ {                                    │
        │   "symbol": "GOOGL",                 │
        │   "price": "178.45",                 │
        │   "lastUpdated": "2026-06-07"        │
        │ }                                    │
        └──────────────────────────────────────┘
```

---

### B. Get Favorites with Live Prices - `GET /api/v1/stocks/favorites`

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ POSTMAN REQUEST                                                             │
│ ────────────────                                                            │
│ Method: GET                                                                 │
│ URL: http://localhost:8082/api/v1/stocks/favorites                           │
└────────────────────────┬────────────────────────────────────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────────┐
        │  StockController                     │
        │  @GetMapping("/favorites")           │
        │                                      │
        │  1. Record start time                │
        │  2. Call stockService.               │
        │     getFavoritesWithLivePrices()     │
        └────────────────┬─────────────────────┘
                         │
                         ▼
        ┌──────────────────────────────────────────────┐
        │  StockService.getFavoritesWithLivePrices()   │
        │                                              │
        │  1. Query Database: findAll()                │
        │     └─ Get all FavoriteStock entities        │
        │                                              │
        │  2. Stream.map() over favorites:             │
        │     For each favorite (e.g., "GOOGL"):       │
        │        │                                     │
        │        ▼                                     │
        │     getStockForSymbol("GOOGL")               │
        │        ├─ Check cache: "stocks" "GOOGL"      │
        │        │                                     │
        │        ├─ IF CACHED: Return immediately      │
        │        │                                     │
        │        └─ IF NOT CACHED:                     │
        │           ├─ Call StockClient                │
        │           ├─ Call Alpha Vantage API          │
        │           ├─ Cache result                    │
        │           └─ Return StockResponse            │
        │                                              │
        │  3. Collect all StockResponse objects        │
        │     into List<StockResponse>                 │
        │                                              │
        │  Result:                                     │
        │  [                                           │
        │    { symbol: "GOOGL", price: "178.45", ... },│
        │    { symbol: "MSFT", price: "425.30", ... }, │
        │    { symbol: "AAPL", price: "195.87", ... }  │
        │  ]                                           │
        └──────────────┬───────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │  StockController (continued)         │
        │                                      │
        │  3. Calculate duration               │
        │  4. Log: "GET /favorites returned    │
        │          3 stocks in XXms"           │
        │  5. Return List<StockResponse>       │
        └──────────────┬───────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────┐
        │ POSTMAN RESPONSE                     │
        │ ────────────────────                 │
        │ Status: 200 OK                       │
        │ Content-Type: application/json       │
        │                                      │
        │ [                                    │
        │   {                                  │
        │     "symbol": "GOOGL",               │
        │     "price": "178.45",               │
        │     "lastUpdated": "2026-06-07"      │
        │   },                                 │
        │   {                                  │
        │     "symbol": "MSFT",                │
        │     "price": "425.30",               │
        │     "lastUpdated": "2026-06-07"      │
        │   },                                 │
        │   {                                  │
        │     "symbol": "AAPL",                │
        │     "price": "195.87",               │
        │     "lastUpdated": "2026-06-07"      │
        │   }                                  │
        │ ]                                    │
        └──────────────────────────────────────┘
```

---

### C. Add Favorite Stock - `POST /api/v1/stocks/favorites`

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ POSTMAN REQUEST                                                              │
│ ────────────────                                                             │
│ Method: POST                                                                 │
│ URL: http://localhost:8082/api/v1/stocks/favorites                            │
│ Content-Type: application/json                                               │
│                                                                              │
│ Body:                                                                        │
│ {                                                                            │
│   "symbol": "MSFT"                                                           │
│ }                                                                            │
└────────────────────────┬───────────────────────────────────────────────────┘
                         │
                         ▼
        ┌─────────────────────────────────────────┐
        │  StockController                        │
        │  @PostMapping("/favorites")             │
        │                                         │
        │  1. Receive FavoriteStockRequest        │
        │     { symbol: "MSFT" }                  │
        │  2. Call stockService.addFavorite()     │
        └────────────────┬────────────────────────┘
                         │
                         ▼
        ┌─────────────────────────────────────────┐
        │  StockService.addFavorite()             │
        │  @Transactional                         │
        │                                         │
        │  1. Check if exists:                    │
        │     favoriteStockRepository             │
        │     .existsByStockSymbol("MSFT")        │
        │        │                                │
        │        ├─ EXISTS:                       │
        │        │  Throw FavoriteAlreadyNotifies │
        │        │  Exception                     │
        │        │                                │
        │        └─ NOT EXISTS:                   │
        │           Create new entity             │
        │                                         │
        │  2. Build FavoriteStock entity:         │
        │     {                                   │
        │       stockSymbol: "MSFT"               │
        │     }                                   │
        │                                         │
        │  3. Save to DB via Repository:          │
        │     favoriteStockRepository.save()      │
        │     └─ H2 INSERT query executed         │
        │                                         │
        │  4. Return saved entity                 │
        │     {                                   │
        │       id: 2,                            │
        │       stockSymbol: "MSFT"               │
        │     }                                   │
        └──────────────┬───────────────────────────┘
                       │
                       ▼
        ┌─────────────────────────────────────────┐
        │  StockController (continued)            │
        │                                         │
        │  3. Return ResponseEntity<FavoriteStock>│
        │     Status: 200 OK                      │
        └──────────────┬───────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────────────────────┐
        │ POSTMAN RESPONSE                         │
        │ ──────────────────                       │
        │ Status: 200 OK                           │
        │ Content-Type: application/json           │
        │                                          │
        │ {                                        │
        │   "id": 2,                               │
        │   "stockSymbol": "MSFT"                  │
        │ }                                        │
        └──────────────────────────────────────────┘
        
        OR (if already exists)
        
        ┌──────────────────────────────────────────┐
        │ POSTMAN RESPONSE (ERROR)                 │
        │ ───────────────────────────────────────  │
        │ Status: 400 Bad Request                  │
        │ Content-Type: application/json           │
        │                                          │
        │ {                                        │
        │   "error": "Favorite Already Exists",    │
        │   "message": "MSFT is already in your... │
        │   "status": 400                          │
        │ }                                        │
        └──────────────────────────────────────────┘
```

---

### D. Get Stock Price History - `GET /api/v1/stocks/{stockSymbol}/history?days=7`

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ POSTMAN REQUEST                                                              │
│ ────────────────                                                             │
│ Method: GET                                                                  │
│ URL: http://localhost:8082/api/v1/stocks/GOOGL/history?days=7                │
└────────────────────────┬───────────────────────────────────────────────────┘
                         │
                         ▼
        ┌───────────────────────────────────────────┐
        │  StockController                          │
        │  @GetMapping("/{stockSymbol}/history")    │
        │                                           │
        │  1. Receive: stockSymbol = "GOOGL"        │
        │             days = 7                      │
        │  2. Convert to uppercase: "GOOGL"         │
        │  3. Call stockClient.getStockHistory()    │
        │     (Note: NO caching on history)         │
        └────────────────┬────────────────────────────┘
                         │
                         ▼
        ┌───────────────────────────────────────────┐
        │  StockClient.getStockHistory()            │
        │                                           │
        │  1. WebClient.get()                       │
        │     ├─ function: TIME_SERIES_DAILY        │
        │     ├─ symbol: GOOGL                      │
        │     └─ apikey: xxxxx                      │
        │                                           │
        │  2. HTTP GET to Alpha Vantage:            │
        │     https://www.alphavantage.co/query?    │
        │     function=TIME_SERIES_DAILY&           │
        │     symbol=GOOGL&apikey=xxxxx             │
        │                                           │
        │  3. Response (100+ days of data):         │
        │     {                                     │
        │       "Meta Data": { ... },               │
        │       "Time Series (Daily)": {            │
        │         "2026-06-07": {                   │
        │           "1. open": "175.50",            │
        │           "2. high": "179.20",            │
        │           "3. low": "174.80",             │
        │           "4. close": "178.45",           │
        │           "5. volume": "45328900"         │
        │         },                                │
        │         "2026-06-06": { ... },            │
        │         ... (100+ entries)               │
        │       }                                   │
        │     }                                     │
        │                                           │
        │  4. Return StockHistoryResponse           │
        └────────────────┬────────────────────────────┘
                         │
                         ▼
        ┌───────────────────────────────────────────┐
        │  StockController (continued)              │
        │                                           │
        │  4. Transform Response:                   │
        │     └─ response.timeSeries().entrySet()   │
        │        .limit(7)  ← Limit to 7 days      │
        │        .map() → Convert to                │
        │        DailyStockResponse objects         │
        │                                           │
        │     Result:                               │
        │     [ DailyStockResponse,                 │
        │       DailyStockResponse,                 │
        │       ... (7 total) ]                     │
        │                                           │
        │  5. Return List<DailyStockResponse>       │
        └────────────────┬────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────────────────┐
        │ POSTMAN RESPONSE                           │
        │ ────────────────────                       │
        │ Status: 200 OK                             │
        │ Content-Type: application/json             │
        │                                            │
        │ [                                          │
        │   {                                        │
        │     "date": "2026-06-07",                  │
        │     "open": 175.50,                        │
        │     "close": 178.45,                       │
        │     "high": 179.20,                        │
        │     "low": 174.80,                         │
        │     "volume": 45328900                     │
        │   },                                       │
        │   {                                        │
        │     "date": "2026-06-06",                  │
        │     "open": 173.20,                        │
        │     "close": 175.30,                       │
        │     "high": 176.15,                        │
        │     "low": 173.00,                         │
        │     "volume": 42156700                     │
        │   },                                       │
        │   ...                                      │
        │   (7 total entries)                        │
        │ ]                                          │
        └────────────────────────────────────────────┘
```

---

## 3. CACHING STRATEGY FLOW

```
┌────────────────────────────────────────────────────────────────────────────┐
│ REQUEST COMES IN                                                           │
│ GET /api/v1/stocks/GOOGL                                                   │
└────────────────┬───────────────────────────────────────────────────────────┘
                 │
                 ▼
        ┌────────────────────────────────────┐
        │  Service Method Intercepted        │
        │  @Cacheable(value="stocks",        │
        │             key="GOOGL")           │
        │                                    │
        │  Spring Cache Abstraction checks:  │
        └──────────┬───────────────┬─────────┘
                   │               │
            ┌──────┴──┐        ┌───┴──────┐
            │          │        │          │
            ▼          ▼        ▼          ▼
        CACHE HIT   CACHE MISS  │          │
            │          │        │          │
            │          ├─ Call StockClient│
            │          │        │          │
            │          │   Execute Method │
            │          │        │          │
            │          ├─ Get AlphaVantage │
            │          │ API Response      │
            │          │        │          │
            │          ├─ Parse & Create   │
            │          │ StockResponse     │
            │          │        │          │
            │          └─ STORE IN CACHE   │
            │                   │          │
            └───────────────────┴──────────┘
                        │
                        │
                Return Object (StockResponse)
                        │
                        ▼
            ┌─────────────────────────┐
            │  POSTMAN RECEIVES       │
            │  {                      │
            │    "symbol": "GOOGL",   │
            │    "price": "178.45",   │
            │    "lastUpdated": ...   │
            │  }                      │
            └─────────────────────────┘

TIMING DIFFERENCE:
─────────────────
First Call (CACHE MISS):   ~500ms  (API call + parsing)
Second Call (CACHE HIT):   ~5ms    (from memory)

CACHE INVALIDATION:
──────────────────
- Cleared on application restart
- Manual clear via Spring Cache API (if implemented)
- For production: Use Redis with TTL
```

---

## 4. DETAILED SEQUENCE DIAGRAM

```
Postman          Controller           Service            Client         Database      API
   │                 │                  │                 │               │             │
   │                 │                  │                 │               │             │
   │  GET /stocks/   │                  │                 │               │             │
   │  GOOGL          │                  │                 │               │             │
   ├────────────────>│                  │                 │               │             │
   │                 │                  │                 │               │             │
   │                 │ getStockForSymbol│                 │               │             │
   │                 │ ("GOOGL")        │                 │               │             │
   │                 ├─────────────────>│                 │               │             │
   │                 │                  │                 │               │             │
   │                 │                  │ Check Cache     │               │             │
   │                 │                  │ "stocks":"GOOGL"│               │             │
   │                 │                  ├──────┐          │               │             │
   │                 │                  │      │ (MISS)    │               │             │
   │                 │                  │<─────┘          │               │             │
   │                 │                  │                 │               │             │
   │                 │                  │ getStockQuote()│               │             │
   │                 │                  ├────────────────>│               │             │
   │                 │                  │                 │               │             │
   │                 │                  │                 │ WebClient.get()           │
   │                 │                  │                 ├──────────────────────────>│
   │                 │                  │                 │                 QUERY     │
   │                 │                  │                 │    function:GLOBAL_QUOTE  │
   │                 │                  │                 │    symbol:GOOGL           │
   │                 │                  │                 │    apikey:xxxxx           │
   │                 │                  │                 │                           │
   │                 │                  │                 │ Process                   │
   │                 │                  │                 │<──────────────────────────┤
   │                 │                  │                 │                           │
   │                 │                  │< AlphaVantageResponse                       │
   │                 │                  │    {globalQuote:...}                        │
   │                 │                  │                 │                           │
   │                 │<─ StockResponse  │                 │                           │
   │                 │  {symbol,price}  │                 │                           │
   │                 │                  │[Store in Cache] │                           │
   │                 │                  │                 │                           │
   │<─---- {symbol,price,lastUpdated}───│                 │                           │
   │   HTTP 200 OK   │                  │                 │               │             │
   │                 │                  │                 │               │             │
   └─────────────────────────────────────────────────────────────────────────────────────>
```

---

## 5. QUICK REFERENCE TABLE

| Endpoint | Method | Cache | External API Call | Response Time |
|----------|--------|-------|------------------|----------------|
| `/stocks/{symbol}` | GET | ✅ Yes | On cache miss | 5ms (cached), 500ms+ (fresh) |
| `/stocks/{symbol}/overview` | GET | ✅ Yes | On cache miss | 5ms (cached), 500ms+ (fresh) |
| `/stocks/{symbol}/history` | GET | ❌ No | Always | 500ms+ (always fresh) |
| `/stocks/favorites` | POST | ❌ N/A | No | 10-20ms (DB write) |
| `/stocks/favorites` | GET | ✅ (per stock) | Per stock | 5ms+ depending on # of stocks |

---

## 6. CACHE BEHAVIOR EXAMPLE

```
Scenario: User requests GOOGL stock three times

REQUEST 1 (T=0ms):
─────────────────
GET /api/v1/stocks/GOOGL
    │
    ├─ Cache lookup: "stocks" → "GOOGL" → NOT FOUND
    ├─ Call Alpha Vantage API (takes ~500ms)
    ├─ Get response: {symbol: GOOGL, price: 178.45}
    ├─ Cache result: cache.put("GOOGL", response)
    └─ Return to Postman (500ms total)

REQUEST 2 (T=1000ms):
────────────────────
GET /api/v1/stocks/GOOGL
    │
    ├─ Cache lookup: "stocks" → "GOOGL" → FOUND!
    ├─ Return cached response immediately
    └─ Return to Postman (5ms total) ← 100x faster!

REQUEST 3 (T=2000ms):
────────────────────
GET /api/v1/stocks/GOOGL
    │
    ├─ Cache lookup: "stocks" → "GOOGL" → STILL CACHED
    ├─ Return cached response immediately
    └─ Return to Postman (5ms total)

CACHE SIZE & PERSISTENCE:
─────────────────────────
- Size: Limited by JVM heap memory
- Persistence: In-memory only (lost on restart)
- Suitable for: Development, Demo applications
- For Production: Upgrade to Redis for persistent, distributed cache
```

---

## 7. ERROR HANDLING FLOW

```
┌─────────────────────────────────────────────────────────────────────────┐
│ REQUEST WITH ERROR SCENARIO                                             │
│ POST /api/v1/stocks/favorites                                           │
│ Body: { "symbol": "GOOGL" } (already added before)                      │
└──────────────────┬──────────────────────────────────────────────────────┘
                   │
                   ▼
        ┌──────────────────────────────────────┐
        │  StockController                     │
        │  @PostMapping("/favorites")          │
        │                                      │
        │  parseRequest → FavoriteStockRequest │
        │  call: stockService.addFavorite()    │
        └──────────────────┬───────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────────┐
        │  StockService.addFavorite()              │
        │                                          │
        │  Check:                                  │
        │  favoriteStockRepository                 │
        │  .existsByStockSymbol("GOOGL")           │
        │     → TRUE (already exists!)             │
        │                                          │
        │  throw FavoriteAlreadyExistsException    │
        │       ("GOOGL already in favorites")     │
        └──────────────────┬────────────────────────┘
                           │
                           ▼ (Exception bubbles up)
        ┌──────────────────────────────────────────┐
        │  GlobalExceptionHandler                  │
        │  @ControllerAdvice                       │
        │                                          │
        │  Catches: FavoriteAlreadyExistsException │
        │  Maps to HTTP 400 Bad Request            │
        │                                          │
        │  Creates error response:                 │
        │  {                                       │
        │    "error": "Favorite Already Exists",   │
        │    "message": "GOOGL is already in...",  │
        │    "status": 400                         │
        │  }                                       │
        └──────────────────┬────────────────────────┘
                           │
                           ▼
        ┌──────────────────────────────────────────────┐
        │ POSTMAN RESPONSE (ERROR)                     │
        │                                              │
        │ Status: 400 Bad Request                      │
        │ Content-Type: application/json               │
        │                                              │
        │ {                                            │
        │   "error": "Favorite Already Exists",        │
        │   "message": "GOOGL is already in favorites",│
        │   "status": 400                              │
        │ }                                            │
        └──────────────────────────────────────────────┘
```

---

## 8. DATABASE INTERACTION FLOW

```
┌─────────────────────────────────────────────────────────────────┐
│ POST /api/v1/stocks/favorites (Add to Favorites)               │
└──────────────────┬──────────────────────────────────────────────┘
                   │
                   ▼
        ┌────────────────────────────────┐
        │  StockService.addFavorite()    │
        │                                │
        │  1. Check existence:           │
        │     SELECT COUNT(*)            │
        │     FROM favorite_stocks       │
        │     WHERE stock_symbol = 'MSFT'│
        │           │                    │
        │           ├─ Row count = 0     │
        │           │  Continue...       │
        │           │                    │
        │           └─ Row count > 0     │
        │              Throw Exception   │
        │                                │
        │  2. Create entity:             │
        │     FavoriteStock {            │
        │       stockSymbol: "MSFT"      │
        │     }                          │
        │                                │
        │  3. Save to Repository:        │
        │     INSERT INTO favorite_stocks│
        │     (stock_symbol)             │
        │     VALUES ('MSFT')            │
        │                    │           │
        │                    ▼           │
        │  4. Return saved entity:       │
        │     {                          │
        │       id: 2,                   │
        │       stockSymbol: "MSFT"      │
        │     }                          │
        └────────────────┬───────────────┘
                         │
                         ▼
        Postman receives result
```

---

## 9. APPLICATION STARTUP FLOW

```
StartApplication
        │
        ▼
@SpringBootApplication
        │
        ├─ Enable Caching: @EnableCaching
        │
        ├─ Initialize Context
        │
        ├─ Load Properties: application.properties
        │  ├─ Alpha Vantage API Key
        │  ├─ Alpha Vantage Base URL
        │  ├─ Database Configuration (H2)
        │  ├─ Server Port (8082)
        │  └─ Cache Configuration
        │
        ├─ Create Beans
        │  ├─ WebClient (for RestAPI calls)
        │  ├─ DataSource (H2 Database)
        │  ├─ SessionFactory (JPA/Hibernate)
        │  ├─ StockController
        │  ├─ StockService
        │  ├─ StockClient
        │  ├─ FavoriteStockRepository
        │  └─ Cache Manager (Simple In-Memory)
        │
        ├─ Initialize Database Schema
        │  ├─ spring.jpa.hibernate.ddl-auto=update
        │  └─ Create table: favorite_stocks if not exists
        │
        ├─ Initialize Caches
        │  ├─ Cache: "stocks" (empty)
        │  └─ Cache: "stockOverviews" (empty)
        │
        ├─ Start Embedded Server
        │  └─ Server listening on http://localhost:8082
        │
        └─ Ready for Requests ✓
```

---

## Key Takeaways

### Data Flow Summary:
1. **Postman** sends HTTP GET/POST request
2. **Controller** receives and validates request, logs timing
3. **Service** implements business logic, manages caching via `@Cacheable`
4. **Cache** intercepts calls:
   - **HIT**: Returns instantly (5ms)
   - **MISS**: Proceeds to next layer
5. **Repository** (if needed) queries H2 Database
6. **Client** (if needed) makes HTTP call to Alpha Vantage API
7. **Response** is cached and returned to **Postman**

### Performance Tips:
- First call to a stock: ~500-800ms (external API call)
- Second+ calls: ~5-10ms (cached)
- Get Favorites can trigger multiple API calls (affected by rate limits)
- Use cache hits to reduce Alpha Vantage API calls (5 calls/min limit)

### Technology Stack:
- **REST Framework**: Spring Boot WebMvc
- **Reactive Client**: Spring WebFlux (WebClient)
- **Database**: H2 In-Memory
- **Cache**: Spring Cache Abstraction (Simple)
- **ORM**: JPA/Hibernate
- **JSON**: Jackson (automatic serialization)

---

**Generated**: June 13, 2026  
**Application Version**: Stock Tracker v0.0.1  
**Framework**: Spring Boot 4.0.6 | Java 21

