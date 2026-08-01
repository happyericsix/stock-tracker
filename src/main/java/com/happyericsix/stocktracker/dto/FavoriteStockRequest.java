package com.happyericsix.stocktracker.dto;

public class FavoriteStockRequest {
    private String symbol;

    public FavoriteStockRequest() {}

    public FavoriteStockRequest(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}