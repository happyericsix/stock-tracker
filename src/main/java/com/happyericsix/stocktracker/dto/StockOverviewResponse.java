package com.happyericsix.stocktracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record StockOverviewResponse(
        @JsonProperty("Symbol") String symbol,
        @JsonProperty("Name") String name,
        @JsonProperty("Description") String description,
        @JsonProperty("Sector") String sector,
        @JsonProperty("Industry") String industry,
        @JsonProperty("MarketCapitalization") String marketCapitalization,
        @JsonProperty("PERatio") String peRatio,
        @JsonProperty("DividendYield") String dividendYield
) implements Serializable {
    private static final long serialVersionUID = 1L;
}