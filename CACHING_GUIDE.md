# Stock Tracker - 缓存与速率限制指南

## 关于速率限制

你遇到了 **AlphaVantage API 的速率限制**（免费版每分钟 5 次请求）。达到限制后，API 会返回空报价，应用会记录警告并返回占位响应：
- `price: "0.0"`
- `lastUpdated: null`

这是**预期行为**，用于防止 API 暂时不可用时出错。

## 缓存的工作原理

**缓存已启用**，可加快响应速度：

### 已缓存的接口
1. **GET `/api/v1/stocks/{symbol}`** — 按股票代码缓存实时报价
2. **GET `/api/v1/stocks/{symbol}/overview`** — 按股票代码缓存公司概况

### 缓存键
每个缓存条目以**股票代码**为键。例如：
- 第一次调用 `GET /AAPL` → 调用 API（慢，约 500ms-2s）
- 第二次调用 `GET /AAPL` → 命中缓存（快，<5ms）
- 第一次调用 `GET /GOOGL` → 新建缓存条目（再次调用 API）

### 缓存类型
- **内存缓存**（Spring 内置的 ConcurrentHashMap）
- 可用缓存区域：`stocks`、`stockOverviews`
- 在 `application.properties` 中配置

## 测试缓存效果

### 方式一：使用测试脚本

```powershell
# 在项目根目录下运行
.\test-cache.ps1
```

脚本会：
- 对同一个股票代码发起 3 次请求
- 展示每次的响应时间
- 演示第 2-3 次请求的缓存加速效果

### 方式二：用 Postman 手动测试

1. **启动应用：**
   ```bash
   mvn spring-boot:run
   ```

2. **对同一个股票代码发起 3 次 GET 请求：**
   ```
   GET http://localhost:8082/api/v1/stocks/AAPL
   ```

3. **观察响应时间：**
   - 请求 1：慢（调用 API）
   - 请求 2-3：快（命中缓存）

4. **查看服务端日志** 中的缓存活动：
   ```
   [INFO] Fetching stock quote for symbol: AAPL (cache miss)
   [INFO] Successfully fetched quote for AAPL. Duration: 1234ms
   [INFO] GET /AAPL returned in 1234ms (price: 235.50)
   ```

   后续请求会显示：
   ```
   [INFO] GET /AAPL returned in 2ms (price: 235.50)
   ```

## 处理速率限制

### 当前处理方式
达到速率限制（每分钟 5 次）时：
- API 不返回数据
- 缓存存储占位响应（0.0, null）
- 后续请求立即返回缓存的占位数据

### 建议：实现指数退避
为了更好地处理速率限制，后续可以考虑：
1. 添加 Resilience4j 的 `@Retry` 和 `@CircuitBreaker` 注解
2. 实现带指数退避的重试逻辑
3. 在 API 被限流时延长缓存时间
4. 向客户端返回 HTTP 429（Too Many Requests）

## 配置项

**缓存设置**（在 `application.properties` 中）：
```properties
spring.cache.type=simple
spring.cache.cache-names=stocks,stockOverviews
logging.level.org.springframework.cache=DEBUG
```

**如需禁用缓存：**
- 从 `StocktrackerApplication.java` 中移除 `@EnableCaching`
- 在 `application.properties` 中设置 `spring.cache.type=none`

**如需启用外部缓存（Redis、Memcached）：**
- 添加依赖：`spring-boot-starter-data-redis`
- 修改：`spring.cache.type=redis`
- 更新 Redis 连接配置

## 改进计划

### 第一阶段（当前已完成）
? 基于 @Cacheable 的基础内存缓存

### 第二阶段（待办）
- [ ] 添加指数退避重试逻辑
- [ ] API 熔断器模式（Circuit Breaker）
- [ ] 缓存过期策略（基于 TTL）
- [ ] 速率限制错误响应（HTTP 429）

### 第三阶段（将来）
- [ ] 切换为 Redis 分布式缓存
- [ ] 缓存监控统计接口
- [ ] 热门股票缓存预热策略

## API 接口

### 获取股票报价（已缓存）
```
GET /api/v1/stocks/{symbol}
Response: { symbol, price, lastUpdated }
```

### 获取公司概况（已缓存）
```
GET /api/v1/stocks/{symbol}/overview
Response: { company info, metrics, etc }
```

### 获取自选股列表（使用缓存）
```
GET /api/v1/stocks/favorites
Response: List<{ symbol, price, lastUpdated }>
Note: Each favorite stock lookup uses the cache
```

### 添加自选股（未缓存）
```
POST /api/v1/stocks/favorites
Body: { "symbol": "AAPL" }
Response: { id, stockSymbol }
```

## 常见问题排查

### 问题：返回 "0.0" 价格和 null 更新时间
**原因：** 达到 AlphaVantage 速率限制（免费版每分钟 5 次）
**解决办法：**
- 等待 1 分钟后重新请求
- 考虑升级到 AlphaVantage 付费计划以获取更高限额

### 问题：没看到缓存加速效果
**检查以下事项：**
1. 是否请求的是同一个股票代码（缓存按股票代码区分，不是全局的）
2. 服务端日志中 "cache miss" 和 "cache hit" 的消息
3. 响应时间差异（命中缓存后应快 100 倍以上）

### 问题：缓存不生效
**请确认：**
- `StocktrackerApplication` 上是否已添加 `@EnableCaching`
- 服务方法上是否已添加 `@Cacheable` 注解
- `application.properties` 中是否设置了 `spring.cache.type=simple`
- 是否已重新构建/部署应用

## 测试输出示例

```
? Call 1 (CACHE MISS): 1234ms - API call
? Call 2 (CACHE HIT):     2ms - Instant
? Call 3 (CACHE HIT):     1ms - Instant

Speedup: 600x faster on cached calls!
```