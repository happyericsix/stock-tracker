package com.abhinav.stocktracker.service;

import com.abhinav.stocktracker.client.StockClient;
import com.abhinav.stocktracker.dto.AlphaVantageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.*;

@SpringBootTest
class StockServiceCacheTest {

    @Autowired
    private StockService stockService;

    @MockitoBean
    private StockClient stockClient;

    @Test
    void shouldUseCacheForRepeatedRequests() {

        AlphaVantageResponse response =
                new AlphaVantageResponse(
                        new AlphaVantageResponse.GlobalQuote(
                                "AAPL",
                                "200.00",
                                "2025-06-14"
                        )
                );

        when(stockClient.getStockQuote("AAPL"))
                .thenReturn(response);

        stockService.getStockForSymbol("AAPL");
        stockService.getStockForSymbol("AAPL");

        verify(stockClient, times(1))
                .getStockQuote("AAPL");
    }
}