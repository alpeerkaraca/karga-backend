package com.alpeerkaraca.authservice.controller;

import com.alpeerkaraca.authservice.dto.UserLoginRequest;
import com.alpeerkaraca.authservice.dto.UserRegisterRequest;
import com.alpeerkaraca.authservice.repository.UserRepository;
import com.alpeerkaraca.authservice.service.AuthService;
import com.alpeerkaraca.common.annotation.RateLimit;
import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.common.dto.TokenPair;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UserRepository userRepository;
    @RateLimit(key = "register", limit = 3, duration = 3600)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null,
                        "Your account has been created successfully. " +
                                "We'll send you an email after activation.")
                );
    }
    @RateLimit(key = "login", limit = 5, duration = 60)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenPair>> loginUser(@Valid @RequestBody UserLoginRequest request) {
        TokenPair tokenPair = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenPair, "Login successful."));
    }
}
