package com.abhinav.stocktracker.service;

import com.abhinav.stocktracker.client.StockClient;
import com.abhinav.stocktracker.dto.AlphaVantageResponse;
import com.abhinav.stocktracker.dto.StockResponse;
import com.abhinav.stocktracker.entity.FavoriteStock;
import com.abhinav.stocktracker.exception.FavoriteAlreadyExistsException;
import com.abhinav.stocktracker.repository.FavoriteStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockClient stockClient;

    @Mock
    private FavoriteStockRepository favoriteStockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void shouldReturnStockResponse() {

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

        StockResponse result =
                stockService.getStockForSymbol("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.symbol());
        assertEquals("200.00", result.price());
        assertEquals("2025-06-14", result.lastUpdated());

        verify(stockClient, times(1))
                .getStockQuote("AAPL");
    }

    @Test
    void shouldReturnDefaultResponseWhenApiReturnsNull() {

        when(stockClient.getStockQuote("AAPL"))
                .thenReturn(null);

        StockResponse result =
                stockService.getStockForSymbol("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.symbol());
        assertEquals("0.0", result.price());
        assertNull(result.lastUpdated());

        verify(stockClient, times(1))
                .getStockQuote("AAPL");
    }

    @Test
    void shouldReturnDefaultResponseWhenGlobalQuoteIsNull() {

        AlphaVantageResponse response =
                new AlphaVantageResponse(null);

        when(stockClient.getStockQuote("AAPL"))
                .thenReturn(response);

        StockResponse result =
                stockService.getStockForSymbol("AAPL");

        assertNotNull(result);
        assertEquals("AAPL", result.symbol());
        assertEquals("0.0", result.price());
        assertNull(result.lastUpdated());

        verify(stockClient, times(1))
                .getStockQuote("AAPL");
    }

    @Test
    void shouldThrowExceptionWhenFavoriteAlreadyExists() {

        when(favoriteStockRepository.existsByStockSymbol("AAPL"))
                .thenReturn(true);

        assertThrows(
                FavoriteAlreadyExistsException.class,
                () -> stockService.addFavorite("AAPL")
        );

        verify(favoriteStockRepository, times(1))
                .existsByStockSymbol("AAPL");

        verify(favoriteStockRepository, never())
                .save(any());
    }

    @Test
    void shouldSaveFavoriteStock() {

        when(favoriteStockRepository.existsByStockSymbol("AAPL"))
                .thenReturn(false);

        FavoriteStock savedStock =
                FavoriteStock.builder()
                        .id(1L)
                        .stockSymbol("AAPL")
                        .build();

        when(favoriteStockRepository.save(any(FavoriteStock.class)))
                .thenReturn(savedStock);

        FavoriteStock result =
                stockService.addFavorite("AAPL");

        ArgumentCaptor<FavoriteStock> captor =
                ArgumentCaptor.forClass(FavoriteStock.class);

        verify(favoriteStockRepository)
                .save(captor.capture());

        FavoriteStock captured = captor.getValue();

        assertEquals("AAPL", captured.getStockSymbol());

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("AAPL", result.getStockSymbol());
    }

    @Test
    void shouldReturnFavoritesWithLivePrices() {

        FavoriteStock favoriteStock =
                FavoriteStock.builder()
                        .stockSymbol("AAPL")
                        .build();

        when(favoriteStockRepository.findAll())
                .thenReturn(List.of(favoriteStock));

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

        List<StockResponse> result =
                stockService.getFavoritesWithLivePrices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).symbol());
        assertEquals("200.00", result.get(0).price());

        verify(favoriteStockRepository, times(1))
                .findAll();

        verify(stockClient, times(1))
                .getStockQuote("AAPL");
    }
}