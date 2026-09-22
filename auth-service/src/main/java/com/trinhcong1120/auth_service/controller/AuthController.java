package com.trinhcong1120.auth_service.controller;

import com.trinhcong1120.auth_service.dto.LoginRequest;
import com.trinhcong1120.auth_service.dto.LoginResponse;
import com.trinhcong1120.auth_service.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}