package com.stockpilot.controller;

import com.stockpilot.dto.AuthDtos.AuthResponse;
import com.stockpilot.dto.AuthDtos.LoginRequest;
import com.stockpilot.dto.AuthDtos.RegisterRequest;
import com.stockpilot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controllers stay thin: map HTTP to a service call and back. No business logic here.
 * @Valid triggers the Bean Validation annotations on the DTO; failures become a 400
 * via GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
