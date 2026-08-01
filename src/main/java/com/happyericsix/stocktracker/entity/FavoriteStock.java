package com.happyericsix.stocktracker.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "favorite_stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stock_symbol", "user_id"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FavoriteStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String stockSymbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Double buyPrice;

    private Integer quantity;

    private String buyDate;

}
