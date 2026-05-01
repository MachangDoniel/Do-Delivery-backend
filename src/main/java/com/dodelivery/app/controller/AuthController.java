package com.dodelivery.app.controller;

import com.dodelivery.app.dto.request.LoginRequest;
import com.dodelivery.app.dto.request.RefreshTokenRequest;
import com.dodelivery.app.dto.request.RegisterRequest;
import com.dodelivery.app.dto.request.SendOtpRequest;
import com.dodelivery.app.dto.response.ApiResponse;
import com.dodelivery.app.dto.response.AuthResponse;
import com.dodelivery.app.dto.response.OtpResponse;
import com.dodelivery.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * All endpoints are public — no JWT required.
 */
@RestController
@RequestMapping(path = "/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     * Returns a mock OTP when app.otp.mock-enabled=true (dev mode).
     */
    @PostMapping(path = "/register")
    public ResponseEntity<ApiResponse<OtpResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        OtpResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Request a fresh OTP for an existing user (subsequent logins).
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        OtpResponse response = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * Verify OTP and receive JWT access + refresh tokens.
     * This is the primary login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * Rotate an expired access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }
}
