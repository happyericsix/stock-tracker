package com.abhinav.stocktracker.dto;

import java.util.List;

/**
 * 股票搜索响应（匹配 Python StockSearchResponse）。
 */
public record StockSearchResponse(String keyword, int count, List<StockSearchItem> results) {}
