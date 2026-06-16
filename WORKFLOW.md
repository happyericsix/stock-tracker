# Stock Tracker - 请求到响应的工作流程

完整的层次结构和流程图，展示请求如何从 Postman 经过各层到达最终的 JSON 响应。

---

## 1. 分层架构

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            表现层（PRESENTATION LAYER）                        │
│                         (Postman / REST Client)                              │
│                      GET /api/v1/stocks/GOOGL                                │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │
                                   │ HTTP 请求
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                            API 层（API LAYER）                                 │
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
                                   │ 调用方法
                                   ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                          业务逻辑层（BUSINESS LOGIC LAYER）                    │
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
        │ 缓存检查                             │ 缓存未命中
        ▼                                     ▼
    ┌─────────────────┐            ┌──────────────────────────┐
    │   缓存命中       │            │    客户端层              │
    │   CACHE HIT     │            │  CLIENT LAYER            │
    │                 │            │                          │
    │  返回缓存数据    │            │  StockClient             │
    │  Return cached  │            │  调用外部 API            │
    │  5-10ms         │            │  External API Call       │
    └─────────────────┘            │  500-800ms               │
                                   └──────────┬───────────────┘
                                              │
                                              ▼
                                   ┌──────────────────────┐
                                   │  Alpha Vantage API    │
                                   │  (外部股票数据源)      │
                                   └──────────────────────┘
                                              │
                                              ▼
                                   ┌──────────────────────┐
                                   │   数据层              │
                                   │  DATA LAYER           │
                                   │                      │
                                   │  FavoriteStockRepo    │
                                   │  ┌─ 查数据库 (查)    │
                                   │  └─ 存数据库 (增删)  │
                                   │                      │
                                   │  H2 内存数据库        │
                                   └──────────────────────┘
                                              │
                                              ▼
                                   ┌──────────────────────┐
                                   │    JSON 响应          │
                                   │    返回给客户端       │
                                   └──────────────────────┘
```

---

## 2. 获取实时股价 — 请求流程

```
Postman                          StockController              StockService                  StockClient                Alpha Vantage
  │                                    │                           │                            │                        │
  │  GET /api/v1/stocks/GOOGL          │                           │                            │                        │
  │───────────────────────────────────►│                           │                            │                        │
  │                                    │                           │                            │                        │
  │                                    │  getStockForSymbol()      │                            │                        │
  │                                    │──────────────────────────►│                            │                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  检查缓存 stocks:GOOGL     │                        │
  │                                    │                           │  ──────────────────────────►(缓存未命中)              │
  │                                    │                           │                            │                        │
  │                                    │                           │  getStockQuote(GOOGL)       │                        │
  │                                    │                           │────────────────────────────►│                        │
  │                                    │                           │                            │  HTTP 请求              │
  │                                    │                           │                            │────────────────────────►│
  │                                    │                           │                            │                        │
  │                                    │                           │                            │  JSON 响应              │
  │                                    │                           │                            │◄────────────────────────│
  │                                    │                           │                            │                        │
  │                                    │                           │◄────────────────────────────│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  存入缓存 stocks:GOOGL     │                        │
  │                                    │                           │                            │                        │
  │                                    │◄──────────────────────────│                            │                        │
  │                                    │                           │                            │                        │
  │  JSON 响应                          │                           │                            │                        │
  │◄───────────────────────────────────│                           │                            │                        │
  │                                    │                           │                            │                        │
```

---

## 3. 获取股票概况 — 请求流程

```
Postman                          StockController              StockService                  StockClient                Alpha Vantage
  │                                    │                           │                            │                        │
  │  GET /api/v1/stocks/MSFT/overview  │                           │                            │                        │
  │───────────────────────────────────►│                           │                            │                        │
  │                                    │                           │                            │                        │
  │                                    │  getStockOverviewFor()    │                            │                        │
  │                                    │──────────────────────────►│                            │                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  检查缓存 stockOverviews   │                        │
  │                                    │                           │  ──────────────────────────►(缓存命中)               │
  │                                    │                           │◄────────────────────────────│                        │
  │                                    │◄──────────────────────────│                            │                        │
  │  JSON 响应                          │                           │                            │                        │
  │◄───────────────────────────────────│                           │                            │                        │
  │                                    │                           │                            │                        │
```

---

## 4. 获取历史行情 — 请求流程

```
Postman                          StockController              StockService                  StockClient                Alpha Vantage
  │                                    │                           │                            │                        │
  │  GET /api/v1/stocks/AAPL/history   │                           │                            │                        │
  │  ?days=7                           │                           │                            │                        │
  │───────────────────────────────────►│                           │                            │                        │
  │                                    │                           │                            │                        │
  │                                    │  getHistory("AAPL", 7)   │                            │                        │
  │                                    │──────────────────────────►│                            │                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  getStockHistory("AAPL")   │                        │
  │                                    │                           │────────────────────────────►│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │                            │  HTTP 请求              │
  │                                    │                           │                            │────────────────────────►│
  │                                    │                           │                            │                        │
  │                                    │                           │                            │  JSON 响应              │
  │                                    │                           │                            │◄────────────────────────│
  │                                    │                           │◄────────────────────────────│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  转换为 DailyStockResponse │                        │
  │                                    │                           │  列表                      │                        │
  │                                    │◄──────────────────────────│                            │                        │
  │                                    │                           │                            │                        │
  │  JSON [                            │                           │                            │                        │
  │   {date,open,close,high,low,vol}   │                           │                            │                        │
  │  ]                                │                           │                            │                        │
  │◄───────────────────────────────────│                           │                            │                        │
  │                                    │                           │                            │                        │
```

---

## 5. 添加自选股 — 请求流程

```
Postman                          StockController              StockService               FavoriteStockRepo          H2 数据库
  │                                    │                           │                            │                        │
  │  POST /api/v1/stocks/favorites     │                           │                            │                        │
  │  {"symbol": "MSFT"}               │                           │                            │                        │
  │───────────────────────────────────►│                           │                            │                        │
  │                                    │                           │                            │                        │
  │                                    │  addFavorite("MSFT")      │                            │                        │
  │                                    │──────────────────────────►│                            │                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  existsByStockSymbol()     │                        │
  │                                    │                           │───────────────────────────►│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  SELECT COUNT(*)           │                        │
  │                                    │                           │  FROM favorite_stocks      │                        │
  │                                    │                           │  WHERE stock_symbol='MSFT' │                        │
  │                                    │                           │◄───────────────────────────│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  save(new FavoriteStock)   │                        │
  │                                    │                           │───────────────────────────►│                        │
  │                                    │                           │                            │  INSERT INTO           │
  │                                    │                           │                            │  favorite_stocks       │
  │                                    │                           │◄───────────────────────────│                        │
  │                                    │                           │                            │                        │
  │                                    │◄──────────────────────────│                            │                        │
  │                                    │                           │                            │                        │
  │  {"id": 2, "stockSymbol": "MSFT"} │                           │                            │                        │
  │◄───────────────────────────────────│                           │                            │                        │
  │                                    │                           │                            │                        │
```

---

## 6. 查看自选股列表 — 请求流程

```
Postman                          StockController              StockService               FavoriteStockRepo      StockClient    Alpha Vantage
  │                                    │                           │                            │                        │
  │  GET /api/v1/stocks/favorites      │                           │                            │                        │
  │───────────────────────────────────►│                           │                            │                        │
  │                                    │                           │                            │                        │
  │                                    │  getFavoritesWithPrices() │                            │                        │
  │                                    │──────────────────────────►│                            │                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  findAll()                 │                        │
  │                                    │                           │───────────────────────────►│                        │
  │                                    │                           │  [AAPL, MSFT]              │                        │
  │                                    │                           │◄───────────────────────────│                        │
  │                                    │                           │                            │                        │
  │                                    │                           │  getStockFor("AAPL")       │                        │
  │                                    │                           │────────────────────────────►│                        │
  │                                    │                           │                            │───────────────────────►│
  │                                    │                           │◄────────────────────────────│◄───────────────────────│
  │                                    │                           │                            │                        │
  │                                    │                           │  getStockFor("MSFT")       │                        │
  │                                    │                           │────────────────────────────►│                        │
  │                                    │                           │                            │───────────────────────►│
  │                                    │                           │◄────────────────────────────│◄───────────────────────│
  │                                    │                           │                            │                        │
  │                                    │◄──────────────────────────│                            │                        │
  │                                    │                           │                            │                        │
  │  JSON [{symbol,price,...}]         │                           │                            │                        │
  │◄───────────────────────────────────│                           │                            │                        │
  │                                    │                           │                            │                        │
```

---

## 7. 缓存交互流程

```
                      ┌──────────────────────────────────────────┐
                      │          Controller 层                   │
                      │    StockController                       │
                      │          │                               │
                      │          │ 调用 Service                   │
                      └──────────│───────────────────────────────┘
                                 │
                                 ▼
                      ┌──────────────────────────────────────────┐
                      │          Service 层                      │
                      │    StockService                          │
                      │          │                               │
                      │          │ @Cacheable("stocks")          │
                      │          ▼                               │
                      │    ┌──────────┐                          │
                      │    │          │                          │
                      │    │  缓存代理 │                          │
                      │    │   Proxy   │                          │
                      │    │          │                          │
                      │    └────┬─────┘                          │
                      │         │                                │
                      │    ┌────┴─────┐                          │
                      │    │          │                          │
                      │  ┌─▼──┐    ┌──▼───┐                     │
                      │  │命中│    │未命中│                     │
                      │  │HIT │    │ MISS  │                     │
                      │  └──┬──┘    └──┬───┘                     │
                      │     │         │                          │
                      │     │         ▼                          │
                      │     │    ┌──────────┐                    │
                      │     │    │ 真实方法  │                    │
                      │     │    │Actual    │                    │
                      │     │    │ Method   │                    │
                      │     │    └────┬─────┘                    │
                      │     │         │                          │
                      │     │         ▼                          │
                      │     │    ┌──────────┐                    │
                      │     │    │ 存入缓存  │                    │
                      │     │    │Put to    │                    │
                      │     │    │ Cache    │                    │
                      │     │    └──────────┘                    │
                      │     │         │                          │
                      └─────┴─────────┴──────────────────────────┘
                            │         │
                            ▼         ▼
                      ┌──────────────────────┐
                      │     返回响应          │
                      │  Return Response     │
                      │                      │
                      │  命中: 5-10ms        │
                      │  未命中: 500-800ms   │
                      └──────────────────────┘
```

---

## 8. 数据库交互流程

```
┌─────────────────────────────────────────────────────────────────┐
│ POST /api/v1/stocks/favorites (添加到自选股)                     │
└──────────────────┬──────────────────────────────────────────────┘
                   │
                   ▼
        ┌────────────────────────────────┐
        │  StockService.addFavorite()    │
        │                                │
        │  1. 检查是否已存在:             │
        │     SELECT COUNT(*)            │
        │     FROM favorite_stocks       │
        │     WHERE stock_symbol = 'MSFT'│
        │           │                    │
        │           ├─ 行数 = 0          │
        │           │  继续添加...       │
        │           │                    │
        │           └─ 行数 > 0          │
        │             抛出异常           │
        │                                │
        │  2. 创建实体:                  │
        │     FavoriteStock {            │
        │       stockSymbol: "MSFT"      │
        │     }                          │
        │                                │
        │  3. 保存到数据库:              │
        │     INSERT INTO favorite_stocks│
        │     (stock_symbol)             │
        │     VALUES ('MSFT')            │
        │                    │           │
        │                    ▼           │
        │  4. 返回已保存的实体:          │
        │     {                          │
        │       id: 2,                   │
        │       stockSymbol: "MSFT"      │
        │     }                          │
        └────────────────┬───────────────┘
                         │
                         ▼
        Postman 收到结果
```

---

## 9. 应用启动流程

```
启动应用
        │
        ▼
@SpringBootApplication
        │
        ├─ 启用缓存: @EnableCaching
        │
        ├─ 初始化上下文
        │
        ├─ 加载配置文件: application.properties
        │  ├─ Alpha Vantage API 密钥（Key）
        │  ├─ Alpha Vantage 接口地址（Base URL）
        │  ├─ 数据库配置（H2）
        │  ├─ 服务器端口（8082）
        │  └─ 缓存配置
        │
        ├─ 创建 Bean
        │  ├─ WebClient (用于调用外部 REST API)
        │  ├─ DataSource (H2 数据库)
        │  ├─ SessionFactory (JPA/Hibernate)
        │  ├─ StockController
        │  ├─ StockService
        │  ├─ StockClient
        │  ├─ FavoriteStockRepository
        │  └─ Cache Manager (内存缓存)
        │
        ├─ 初始化数据库表结构
        │  ├─ spring.jpa.hibernate.ddl-auto=update
        │  └─ 创建表: favorite_stocks (如果不存在)
        │
        ├─ 初始化缓存
        │  ├─ 缓存: "stocks" (空)
        │  └─ 缓存: "stockOverviews" (空)
        │
        ├─ 启动内嵌服务器
        │  └─ 服务器监听 http://localhost:8082
        │
        └─ 就绪，等待请求 ✓
```

---

## 要点总结

### 数据流概要：
1. **Postman** 发送 HTTP GET/POST 请求
2. **Controller** 接收并验证请求，记录耗时日志
3. **Service** 执行业务逻辑，通过 `@Cacheable` 管理缓存
4. **缓存** 拦截调用：
   - **命中（HIT）**：立即返回 (5ms)
   - **未命中（MISS）**：继续往下执行
5. **Repository**（需要时）查询 H2 数据库
6. **Client**（需要时）调用 Alpha Vantage API
7. **响应** 被缓存后返回给 **Postman**

### 性能提示：
- 首次查询某只股票：~500-800ms（调外部 API）
- 第二次及之后：~5-10ms（缓存命中）
- 查看自选股可能触发多次 API 调用（受频率限制影响）
- 善用缓存可以减少 Alpha Vantage API 调用（免费版每分钟 5 次限制）

### 技术栈：
- **REST 框架**：Spring Boot WebMvc
- **响应式客户端**：Spring WebFlux (WebClient)
- **数据库**：H2 内存数据库
- **缓存**：Spring Cache 抽象（Simple 实现）
- **ORM**：JPA/Hibernate
- **JSON**：Jackson（自动序列化）

---

**生成日期**：2026 年 6 月 13 日  
**应用版本**：Stock Tracker v0.0.1  
**框架**：Spring Boot 4.0.6 | Java 21