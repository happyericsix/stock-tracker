package com.abhinav.stocktracker.repository;

import com.abhinav.stocktracker.entity.FavoriteStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteStockRepository extends JpaRepository<FavoriteStock, Long> {

    boolean existsByStockSymbol(String stockSymbol);

}
