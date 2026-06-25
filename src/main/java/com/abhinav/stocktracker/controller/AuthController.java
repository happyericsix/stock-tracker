package com.abhinav.stocktracker.controller;

import com.abhinav.stocktracker.dto.AuthResponse;
import com.abhinav.stocktracker.dto.LoginRequest;
import com.abhinav.stocktracker.dto.RegisterRequest;
import com.abhinav.stocktracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response= authService.register(request);
        // 返回 JWT Token
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse= authService.login(request);
        return ResponseEntity.ok(authResponse);
    }
}