package com.abhinav.stocktracker.exception;

public class FavoriteAlreadyExistsException extends RuntimeException {
    public FavoriteAlreadyExistsException(String stockSymbol) {
        super("Favorite already exists: " + stockSymbol);
    }
}
