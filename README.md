# Stock Tracker

基于 Spring Boot 4 + Vue 3 的股票实时追踪系统，支持沪深 A 股、港股、美股行情查询。
使用腾讯财经 API 作为数据源，Redis + Caffeine 二级缓存加速，JWT 认证。

## 功能特性

- **实时股价查询**：支持 A 股(`sh`/`sz`)、港股(`hk`)、美股
- **公司概况**：获取股票名称、市值、市盈率等信息
- **K 线历史**：日线历史数据查询（分页）
- **自选股管理**：添加/删除自选股，批量查看实时价格
- **JWT 认证**：用户注册登录，接口权限控制
- **二级缓存**：Caffeine（本地） + Redis（分布式）两级缓存
- **前后端分离**：Vue 3 前端 + Nginx 反向代理
- **Docker 部署**：完整 Docker Compose 编排

## 技术栈

| 层级 | 技术 |
|---|---|
| 后端框架 | Spring Boot 4 + Java 21 |
| 前端 | Vue 3 + Vite + Pinia + Vue Router |
| 数据库 | H2 (内存) |
| 缓存 | Caffeine + Redis |
| 认证 | JWT (RSA 签名) |
| 数据源 | 腾讯财经 API |
| 部署 | Docker Compose (Redis + App + Nginx) |

## 快速开始

### 方式一：IDEA 运行（开发推荐）

**前置条件：**
- JDK 21
- Docker（运行 Redis）
- Node.js 18+（运行前端）

**1. 启动 Redis**
```bash
docker run -d --name stock-redis -p 6379:6379 redis:7-alpine
```

**2. 启动后端（IDEA）**
```
在 IDEA 中打开项目 → 运行 StocktrackerApplication
后端启动在 http://localhost:8080
```

**3. 启动前端**
```bash
cd frontend
npm install   # 首次运行
npm run dev   # 开发服务器 → http://localhost:5173
```

### 方式二：Docker Compose 部署

```bash
docker compose up -d
# Redis → localhost:6379
# 后端  → localhost:8080
# 前端  → http://localhost (80端口)
```

## 股票代码格式

| 市场 | 格式 | 示例 |
|---|---|---|
| 沪市 A 股 | `sh + 代码` | `sh600519`（贵州茅台） |
| 深市 A 股 | `sz + 代码` | `sz300750`（宁德时代） |
| 港股 | `hk + 代码` | `hk00700`（腾讯控股） |
| 美股 | 直接代码 | `AAPL`、`MSFT`、`TSLA` |
| 自动识别 | 6 开头→沪市 | `600519` → `sh600519` |

## API 接口

所有接口（除认证外）都需要在请求头携带 JWT Token：
```
Authorization: Bearer <token>
```

### 认证

| 方法 | 路径 | 说明 | 是否需要 Token |
|---|---|---|---|
| POST | `/api/v1/auth/register` | 注册 | 否 |
| POST | `/api/v1/auth/login` | 登录 | 否 |

**注册请求体：**
```json
{"username": "xxx", "email": "x@x.com", "password": "123456"}
```

**登录响应：**
```json
{"token": "eyJ...", "username": "xxx", "email": "x@x.com", "message": "Login successful"}
```

### 股票查询

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/stocks/{symbol}` | 实时股价 |
| GET | `/api/v1/stocks/{symbol}/overview` | 公司概况 |
| GET | `/api/v1/stocks/{symbol}/history?page=0&size=30` | K 线历史 |

**示例：**
```bash
# 查询贵州茅台
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/stocks/sh600519

# 查询苹果
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/stocks/AAPL
```

### 自选股

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/stocks/favorites` | 查看自选股（含实时价） |
| POST | `/api/v1/stocks/favorites` | 添加自选 |
| DELETE | `/api/v1/stocks/favorites/{symbol}` | 删除自选 |

**添加自选：**
```json
{"symbol": "AAPL"}
```

## 项目结构

```
stock-tracker/
├── frontend/                    # Vue 3 前端
│   ├── src/
│   │   ├── api/                 # API 调用层
│   │   ├── pages/               # 页面组件
│   │   │   ├── Login.vue        # 登录/注册
│   │   │   ├── Dashboard.vue    # 搜索 & 自选股
│   │   │   └── StockDetail.vue  # 股票详情
│   │   └── router/              # 路由配置
│   ├── Dockerfile
│   └── package.json
├── src/
│   └── main/java/com/abhinav/stocktracker/
│       ├── cache/               # 二级缓存实现
│       │   └── TwoLevelCache.java
│       ├── client/              # 腾讯 API 客户端
│       │   └── StockClient.java
│       ├── config/              # 配置类
│       │   ├── SecurityConfig.java    # JWT + 安全
│       │   ├── RedisCacheConfig.java   # Redis 缓存
│       │   └── WebClientConfig.java    # HTTP 客户端
│       ├── controller/          # REST 控制器
│       ├── dto/                 # 数据传输对象
│       ├── entity/              # 实体类
│       ├── exception/           # 异常处理
│       ├── repository/          # 数据访问
│       ├── security/            # 用户认证
│       └── service/             # 业务逻辑
├── docker-compose.yml           # Docker 编排
├── DockerFile                   # 后端构建
├── nginx.conf                   # Nginx 反向代理
└── pom.xml
```

## 缓存架构

采用两级缓存策略（Caffeine + Redis）：

```
请求 → ① 查 Caffeine（本地内存，25s 过期）
         ├── 命中 → 直接返回
         └── 未命中 → ② 查 Redis（分布式，30s 过期）
                       ├── 命中 → 回填 Caffeine → 返回
                       └── 未命中 → ③ 调腾讯 API → 回填两级缓存 → 返回
```

- 缓存名称：`stocks`（股价）、`stockOverviews`（概况）
- 启动时自动清理 Redis 脏数据

## 配置说明

`application.properties` 关键配置：
```properties
# 腾讯财经 API
tencent.stock.base.url=http://qt.gtimg.cn
tencent.stock.history.base.url=http://web.ifzq.gtimg.cn

# Redis 连接
spring.data.redis.host=localhost
spring.data.redis.port=6379

# 缓存类型
spring.cache.type=redis

# JWT
jwt.expiration=86400000
```

## 环境要求

- Java 21
- Docker（运行 Redis）
- Node.js 18+（运行前端开发服务器）