package com.abhinav.stocktracker.dto;


import lombok.Builder;

@Builder
public record StockResponse(
        String symbol,
        String price,
        String lastUpdated) {
}