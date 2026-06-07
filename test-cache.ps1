# Cache Test Script - Shows the difference between cache miss and cache hits
# Run this after the app is started on http://localhost:8082

$symbol = "AAPL"  # Change this to test different stocks
$baseUrl = "http://localhost:8082/api/v1/stocks"

Write-Host "========================================" -ForegroundColor Green
Write-Host "Stock Price Cache Demonstration" -ForegroundColor Green
Write-Host "Testing symbol: $symbol" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Clear cache by restarting doesn't apply here, but we'll show the behavior
Write-Host "Call 1: First request (CACHE MISS - hits AlphaVantage API)" -ForegroundColor Yellow
$start = Get-Date
try {
    $response1 = Invoke-RestMethod -Uri "$baseUrl/$symbol" -Method GET -TimeoutSec 30
    $elapsed = (Get-Date) - $start
    Write-Host "  Price: $($response1.price)" -ForegroundColor Cyan
    Write-Host "  Last Updated: $($response1.lastUpdated)" -ForegroundColor Cyan
    Write-Host "  Response Time: $($elapsed.TotalMilliseconds)ms" -ForegroundColor Green
} catch {
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Call 2: Second request (CACHE HIT - returns from cache instantly)" -ForegroundColor Yellow
$start = Get-Date
try {
    $response2 = Invoke-RestMethod -Uri "$baseUrl/$symbol" -Method GET -TimeoutSec 30
    $elapsed = (Get-Date) - $start
    Write-Host "  Price: $($response2.price)" -ForegroundColor Cyan
    Write-Host "  Last Updated: $($response2.lastUpdated)" -ForegroundColor Cyan
    Write-Host "  Response Time: $($elapsed.TotalMilliseconds)ms" -ForegroundColor Green
} catch {
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Call 3: Third request (CACHE HIT - should be fast)" -ForegroundColor Yellow
$start = Get-Date
try {
    $response3 = Invoke-RestMethod -Uri "$baseUrl/$symbol" -Method GET -TimeoutSec 30
    $elapsed = (Get-Date) - $start
    Write-Host "  Price: $($response3.price)" -ForegroundColor Cyan
    Write-Host "  Last Updated: $($response3.lastUpdated)" -ForegroundColor Cyan
    Write-Host "  Response Time: $($elapsed.TotalMilliseconds)ms" -ForegroundColor Green
} catch {
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Note:" -ForegroundColor Cyan
Write-Host "- Call 1 should be slowest (API call + network latency)" -ForegroundColor Cyan
Write-Host "- Calls 2-3 should be much faster (sub-5ms typically)" -ForegroundColor Cyan
Write-Host "- Check server logs for messages like:" -ForegroundColor Cyan
Write-Host "  'Fetching stock quote for symbol: AAPL (cache miss)'" -ForegroundColor Cyan
Write-Host "  'GET /AAPL returned in Xms (price: ...)'" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Green

