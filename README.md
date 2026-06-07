# Stock Tracker

A Spring Boot REST API application for tracking stock prices in real-time using the Alpha Vantage API. The application allows users to fetch live stock quotes, view stock overviews, get historical price data, and maintain a list of favorite stocks with live prices.

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Setup & Installation](#setup--installation)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Caching Strategy](#caching-strategy)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

## Project Overview

Stock Tracker is a REST API that integrates with the **Alpha Vantage API** to provide:

- **Live Stock Quotes**: Fetch current price for any stock symbol
- **Stock Overview**: Get detailed company information
- **Historical Data**: Retrieve daily stock price history
- **Favorites Management**: Save and retrieve favorite stocks with live prices
- **Caching**: In-memory caching for improved performance and API rate-limit compliance

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      REST API Client                         │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Controller                      │
│              (StockController @ /api/v1/stocks)             │
└────────────────────────────┬────────────────────────────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
         ┌──────────────────┐  ┌──────────────────┐
         │  StockService   │  │  StockClient     │
         │   (Business     │  │  (External API   │
         │    Logic)       │  │   Integration)   │
         └────────┬────────┘  └────────┬─────────┘
                  │                    │
         ┌────────┴────────┐    ┌──────┴─────────┐
         ▼                 ▼    ▼                 ▼
    ┌────────────┐  ┌──────────────────┐  ┌──────────────┐
    │  Database  │  │  In-Memory Cache │  │Alpha Vantage │
    │    (H2)    │  │   (Spring Cache) │  │     API      │
    └────────────┘  └──────────────────┘  └──────────────┘
```

### Data Flow Architecture

```
CLIENT REQUEST
       │
       ▼
   CONTROLLER
       │
       ├─── Cache Check ───► HIT ──► Return Cached Value
       │
       └─── Cache MISS ◄─── Hit DB or Call External API
              │
              ├─► Favorites: Fetch from DB → Get live prices → Cache & Return
              │
              └─► Stock Data: Call Alpha Vantage → Parse Response → Cache & Return
```

### Layer Breakdown

1. **Controller Layer** (`StockController`)
   - Handles HTTP requests
   - Logs request duration
   - Normalizes input (converts to uppercase)

2. **Service Layer** (`StockService`)
   - Implements business logic
   - Manages caching via `@Cacheable` annotation
   - Handles favorites management

3. **Client Layer** (`StockClient`)
   - Calls external Alpha Vantage API
   - Uses Spring WebClient (reactive)
   - Builds HTTP requests with parameters

4. **Repository Layer** (`FavoriteStockRepository`)
   - Spring Data JPA interface
   - Handles database operations for favorites

5. **Entity & DTO Layer**
   - `FavoriteStock` - JPA entity (database table)
   - `StockResponse`, `StockOverviewResponse`, `StockHistoryResponse` - DTOs for API responses

## Prerequisites

- **Java**: JDK 21 or higher
- **Maven**: 3.6.0 or higher
- **Git**: For cloning the repository
- **Internet**: Required for Alpha Vantage API calls
- **Alpha Vantage API Key**: Get free at https://www.alphavantage.co/

## Setup & Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/stocktracker.git
cd stocktracker
```

### 2. Configure API Key

Open `src/main/resources/application.properties` and update your Alpha Vantage API key:

```properties
alpha.vantage.api.key=YOUR_API_KEY_HERE
alpha.vantage.base.url=https://www.alphavantage.co/query
```

### 3. Build the Project

```bash
mvn clean install -DskipTests
```

Or with tests (if you have configured test environment):

```bash
mvn clean install
```

## Running the Application

### Option 1: Maven Spring Boot Plugin

```bash
mvn -DskipTests spring-boot:run
```

### Option 2: Run JAR File

```bash
# Build the JAR
mvn clean package -DskipTests

# Run the JAR
java -jar target/stocktracker-0.0.1-SNAPSHOT.jar
```

### Option 3: Run from IDE

- Right-click on `StocktrackerApplication.java`
- Select "Run"

### 4. Verify Application is Running

The application will start on `http://localhost:8082` (port configurable in `application.properties`)

Access H2 Console (optional): `http://localhost:8082/h2-console`

## Project Structure

```
stocktracker/
├── src/
│   ├── main/
│   │   ├── java/com/abhinav/stocktracker/
│   │   │   ├── StocktrackerApplication.java      # Main entry point
│   │   │   ├── controller/
│   │   │   │   └── StockController.java          # REST endpoints
│   │   │   ├── service/
│   │   │   │   └── StockService.java             # Business logic & caching
│   │   │   ├── client/
│   │   │   │   └── StockClient.java              # Alpha Vantage API client
│   │   │   ├── config/
│   │   │   │   └── WebClientConfig.java          # WebClient bean configuration
│   │   │   ├── dto/
│   │   │   │   ├── StockResponse.java
│   │   │   │   ├── StockOverviewResponse.java
│   │   │   │   ├── StockHistoryResponse.java
│   │   │   │   ├── DailyStockResponse.java
│   │   │   │   ├── FavoriteStockRequest.java
│   │   │   │   └── AlphaVantageResponse.java
│   │   │   ├── entity/
│   │   │   │   └── FavoriteStock.java            # JPA entity
│   │   │   ├── repository/
│   │   │   │   └── FavoriteStockRepository.java  # Data access layer
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java   # Exception handling
│   │   │       └── FavoriteAlreadyExistsException.java
│   │   └── resources/
│   │       └── application.properties            # Application configuration
│   └── test/
│       └── java/...
├── pom.xml                                       # Maven dependencies
├── README.md                                     # This file
└── mvnw / mvnw.cmd                               # Maven wrapper
```

## API Endpoints

All endpoints are prefixed with `/api/v1/stocks`

### 1. Get Stock Quote

**Endpoint:** `GET /api/v1/stocks/{stockSymbol}`

**Description:** Fetch the current price and last trading day for a stock symbol

**Parameters:**
- `stockSymbol` (path): Stock symbol (e.g., GOOGL, MSFT, AAPL) - case insensitive

**Response:**
```json
{
  "symbol": "GOOGL",
  "price": "178.45",
  "lastUpdated": "2026-06-07"
}
```

**Workflow:**
1. Client sends GET request with stock symbol
2. Controller receives request and converts symbol to uppercase
3. Service checks if data exists in cache (stocks cache)
4. If cached: return immediately
5. If not cached:
   - Call Alpha Vantage API via StockClient
   - Extract symbol, price, and last trading day from response
   - Cache the result (default TTL: until app restart)
   - Return response
6. Controller logs request duration and price

**Example:**
```bash
curl -X GET "http://localhost:8082/api/v1/stocks/GOOGL"
```

---

### 2. Get Stock Overview

**Endpoint:** `GET /api/v1/stocks/{stockSymbol}/overview`

**Description:** Get detailed company information including PE ratio, market cap, dividend, etc.

**Parameters:**
- `stockSymbol` (path): Stock symbol - case insensitive

**Response:**
```json
{
  "symbol": "GOOGL",
  "assetType": "Common Stock",
  "name": "Alphabet Inc.",
  "description": "Alphabet Inc. is a holding company...",
  "marketCapitalization": "1234567890000",
  "peRatio": "28.45",
  "dividend": "0.00",
  ...
}
```

**Workflow:**
1. Client sends GET request with stock symbol
2. Controller converts symbol to uppercase
3. Service checks if data exists in cache (stockOverviews cache)
4. If cached: return immediately
5. If not cached:
   - Call Alpha Vantage OVERVIEW function via StockClient
   - Cache the result
   - Return response
6. Controller logs request duration

**Example:**
```bash
curl -X GET "http://localhost:8082/api/v1/stocks/GOOGL/overview"
```

---

### 3. Get Stock Price History

**Endpoint:** `GET /api/v1/stocks/{stockSymbol}/history?days=30`

**Description:** Fetch daily stock price history with opening, closing, high, low, and volume

**Parameters:**
- `stockSymbol` (path): Stock symbol - case insensitive
- `days` (query): Number of days of history to return (default: 30)

**Response:**
```json
[
  {
    "date": "2026-06-07",
    "open": "175.50",
    "close": "178.45",
    "high": "179.20",
    "low": "174.80",
    "volume": 45328900
  },
  {
    "date": "2026-06-06",
    "open": "173.20",
    "close": "175.30",
    "high": "176.15",
    "low": "173.00",
    "volume": 42156700
  }
]
```

**Workflow:**
1. Client sends GET request with stock symbol and optional days parameter
2. Controller converts symbol to uppercase
3. StockClient calls Alpha Vantage TIME_SERIES_DAILY function
4. Response contains daily prices for the past 100+ days
5. Controller transforms the response:
   - Parses date, open, close, high, low, volume
   - Limits results to requested number of days (default 30)
   - Returns as list of DailyStockResponse objects
6. No caching is applied to history endpoint (always fresh data)

**Example:**
```bash
curl -X GET "http://localhost:8082/api/v1/stocks/GOOGL/history?days=7"
```

---

### 4. Add Favorite Stock

**Endpoint:** `POST /api/v1/stocks/favorites`

**Description:** Save a stock symbol to the favorites list

**Request Body:**
```json
{
  "symbol": "GOOGL"
}
```

**Response:**
```json
{
  "id": 1,
  "stockSymbol": "GOOGL"
}
```

**Status Codes:**
- `200 OK`: Stock added successfully
- `400 Bad Request`: Stock already exists in favorites (FavoriteAlreadyExistsException)

**Workflow:**
1. Client sends POST request with stock symbol in JSON body
2. Controller passes request to StockService
3. Service checks if symbol already exists in database:
   - If exists: throw FavoriteAlreadyExistsException (HTTP 400)
   - If not: create new FavoriteStock entity
4. Save to database via FavoriteStockRepository
5. Return saved entity with ID
6. Global exception handler catches FavoriteAlreadyExistsException and returns error response

**Example:**
```bash
curl -X POST "http://localhost:8082/api/v1/stocks/favorites" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"GOOGL"}'
```

---

### 5. Get Favorite Stocks with Live Prices

**Endpoint:** `GET /api/v1/stocks/favorites`

**Description:** Fetch all favorite stocks with their current live prices

**Response:**
```json
[
  {
    "symbol": "GOOGL",
    "price": "178.45",
    "lastUpdated": "2026-06-07"
  },
  {
    "symbol": "MSFT",
    "price": "425.30",
    "lastUpdated": "2026-06-07"
  },
  {
    "symbol": "AAPL",
    "price": "195.87",
    "lastUpdated": "2026-06-07"
  }
]
```

**Workflow:**
1. Client sends GET request to /favorites
2. Controller calls StockService.getFavoritesWithLivePrices()
3. Service fetches all FavoriteStock records from database
4. For each favorite stock:
   - Call getStockForSymbol() to get live price (endpoint #1)
   - Benefit from caching: if price was recently fetched, cache is used
   - If not cached: call Alpha Vantage API (respects rate limiting via cache)
5. Collect results into a list of StockResponse objects
6. Controller logs request duration and number of stocks returned
7. Return list to client

**Note:** This endpoint can trigger multiple API calls if prices are not cached. Consider Alpha Vantage rate limits (5 calls/minute for free tier).

**Example:**
```bash
curl -X GET "http://localhost:8082/api/v1/stocks/favorites"
```

---

## Database Schema

### FavoriteStock Table

```sql
CREATE TABLE favorite_stocks (
    id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    stock_symbol VARCHAR(255) NOT NULL UNIQUE
);
```

**Columns:**
- `id`: Auto-incremented primary key
- `stock_symbol`: Stock symbol (e.g., GOOGL, MSFT). Must be unique to prevent duplicates.

**Properties:**
- Database: H2 (in-memory by default)
- DDL: `spring.jpa.hibernate.ddl-auto=update` (automatically creates/updates schema)

## Caching Strategy

The application uses **Spring Cache Abstraction** with **Simple In-Memory Cache** strategy.

### Cache Configuration

Two named caches are configured:
1. **stocks** - Caches individual stock prices (endpoint #1)
2. **stockOverviews** - Caches stock overview data (endpoint #2)

```properties
spring.cache.cache-names=stocks,stockOverviews
spring.cache.type=simple
```

### Caching in StockService

```java
@Cacheable(value = "stocks", key = "#stockSymbol")
public StockResponse getStockForSymbol(final String stockSymbol) { ... }

@Cacheable(value = "stockOverviews", key = "#stockSymbol")
public StockOverviewResponse getStockOverviewForSymbol(final String stockSymbol) { ... }
```

**How it works:**
- First call for a symbol: fetches from Alpha Vantage API → caches result
- Subsequent calls: returns cached value (instant response, no API call)
- Cache is cleared when application restarts

### Benefits

1. **Improved Performance**: Subsequent calls are instant (no network delay)
2. **Reduce API Rate Limit Issues**: Fewer calls to external API
3. **Reduced Latency**: Cached response is returned immediately

### Important Notes

- Cache is in-memory and non-persistent (cleared on app restart)
- For production, consider using Redis or Memcached
- Alpha Vantage Free Tier: 5 API calls/minute, 500 calls/day
  - With caching, multiple users can share the same cached price
  - Only new symbols or after cache expiration trigger API calls

## Configuration

### application.properties

Located at: `src/main/resources/application.properties`

```properties
# Application Name
spring.application.name=stocktracker

# Alpha Vantage Configuration
alpha.vantage.base.url=https://www.alphavantage.co/query
alpha.vantage.api.key=YOUR_API_KEY_HERE

# Database Configuration (H2 In-Memory)
spring.datasource.url=jdbc:h2:mem:stockdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Server Port
server.port=8082

# Cache Configuration
spring.cache.cache-names=stocks,stockOverviews
spring.cache.type=simple

# Logging Configuration
logging.level.com.abhinav.stocktracker=INFO
logging.level.org.springframework.cache=DEBUG
```

### Key Properties

| Property | Default | Description |
|----------|---------|-------------|
| `alpha.vantage.api.key` | `74UO0AK3TRX4QF8K` | Alpha Vantage API key (should be changed to your own) |
| `server.port` | `8082` | Server port |
| `spring.cache.type` | `simple` | Cache implementation (simple = in-memory) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-update database schema |

## Troubleshooting

### 1. API Returns Price "0.0" with null lastUpdated

**Cause:** Alpha Vantage API returned a non-quote response (usually rate-limited).

**Solution:**
- Check API key is correct in `application.properties`
- Wait a few minutes and retry (free tier has 5 calls/min limit)
- Check Alpha Vantage status at https://www.alphavantage.co/

**To debug:**
- Add to `application.properties`: `logging.level.com.abhinav.stocktracker.client=DEBUG`
- Check logs for raw API response

### 2. Port 8082 Already in Use

**Solution:**
- Change port in `application.properties`: `server.port=8083`
- Or kill process using port 8082

**On Windows (PowerShell):**
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8082).OwningProcess | Stop-Process
```

### 3. "Favorite Already Exists" Error

**Cause:** You're trying to add the same stock symbol to favorites twice.

**Solution:** Delete the existing favorite or check duplicate in database using H2 console.

### 4. Database Reset

**To reset the H2 in-memory database:**
- Simply restart the application (data is cleared since it's in-memory)

**To persist data in a file** instead of in-memory:
- Change `application.properties`:
  ```properties
  spring.datasource.url=jdbc:h2:file:./data/stockdb
  ```

### 5. Application Won't Start

**Check logs for:**
- `Port 8082 already in use` → Change port
- `Cannot resolve API key` → Update `application.properties`
- `Compilation error` → Run `mvn clean compile`

## API Testing Examples

### Using cURL

```bash
# Get stock price
curl -X GET "http://localhost:8082/api/v1/stocks/GOOGL"

# Get stock overview
curl -X GET "http://localhost:8082/api/v1/stocks/MSFT/overview"

# Get 7 days of history
curl -X GET "http://localhost:8082/api/v1/stocks/AAPL/history?days=7"

# Add to favorites
curl -X POST "http://localhost:8082/api/v1/stocks/favorites" \
  -H "Content-Type: application/json" \
  -d '{"symbol":"GOOGL"}'

# Get favorites with live prices
curl -X GET "http://localhost:8082/api/v1/stocks/favorites"
```

### Using Postman

1. **GET Request - Stock Quote**
   - URL: `http://localhost:8082/api/v1/stocks/GOOGL`
   - Method: GET
   - Headers: None
   - Body: None

2. **POST Request - Add Favorite**
   - URL: `http://localhost:8082/api/v1/stocks/favorites`
   - Method: POST
   - Headers: `Content-Type: application/json`
   - Body (raw JSON):
     ```json
     {
       "symbol": "MSFT"
     }
     ```

## Technologies Used

- **Java 21** - Programming language
- **Spring Boot 4.0.6** - Framework
- **Spring Data JPA** - ORM and data access
- **Spring WebFlux** - Reactive HTTP client
- **H2 Database** - In-memory database
- **Lombok** - Reduce boilerplate code
- **Maven** - Build and dependency management

## Dependencies

See `pom.xml` for full list. Key dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Future Improvements

1. **Redis Caching** - Replace in-memory cache with Redis for distributed caching
2. **Rate Limiting** - Implement client-side rate limiter to respect API limits
3. **Retry Logic** - Add exponential backoff for failed API calls
4. **Authentication** - Add JWT or OAuth2 authentication
5. **Pagination** - Add pagination for large result sets
6. **Webhooks** - Real-time price alerts via webhooks
7. **Unit Tests** - Comprehensive unit and integration tests
8. **Docker Support** - Docker image for easy deployment
9. **Scheduled Jobs** - Periodic price updates for favorites

## License

This project is open source and available under the MIT License.

## Contact & Support

For issues or questions, please create an issue on the GitHub repository.

