package com.abhinav.stocktracker.exception;

import com.abhinav.stocktracker.exception.RateLimitException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FavoriteAlreadyExistsException.class)
    public ResponseEntity<String> handleFavoriteAlreadyExists(FavoriteAlreadyExistsException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<String> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(429).body(ex.getMessage());
    }
}
