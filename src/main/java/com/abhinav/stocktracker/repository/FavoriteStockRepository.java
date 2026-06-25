package com.abhinav.stocktracker.repository;

import com.abhinav.stocktracker.entity.FavoriteStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {

    void deleteByStockSymbol(String stockSymbol);

    boolean existsByStockSymbol(String stockSymbol);
    List<FavoriteStock> findByUserId(Long userId);
    void deleteByStockSymbolAndUserId(String stockSymbol, Long userId);
    boolean existsByStockSymbolAndUserId(String stockSymbol, Long userId);

}
