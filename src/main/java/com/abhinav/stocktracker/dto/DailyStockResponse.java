package com.abhinav.stocktracker.dto;

public record DailyStockResponse(
        String date,
        double open,
        double close,
        double high,
        double low,
        long volume
) {}