package com.dodelivery.app.controller;

import com.dodelivery.app.dto.request.LoginRequest;
import com.dodelivery.app.dto.request.RefreshTokenRequest;
import com.dodelivery.app.dto.request.RegisterRequest;
import com.dodelivery.app.dto.request.SendOtpRequest;
import com.dodelivery.app.dto.response.ApiResponse;
import com.dodelivery.app.dto.response.AuthResponse;
import com.dodelivery.app.dto.response.MeResponse;
import com.dodelivery.app.dto.response.OtpResponse;
import com.dodelivery.app.exception.BusinessException;
import com.dodelivery.app.security.AppUserDetails;
import com.dodelivery.app.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * All endpoints except /me are public — no JWT required.
 */
@RestController
@RequestMapping(path = "/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.cookie.access-token-max-age-seconds:900}")
    private long accessTokenMaxAge;

    @Value("${app.cookie.refresh-token-max-age-seconds:604800}")
    private long refreshTokenMaxAge;

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
     * Sets HttpOnly cookies for web clients; also returns tokens in the body for mobile.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse tokens = authService.login(request);
        addAuthCookies(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), tokens));
    }

    /**
     * Rotate an expired access token using a valid refresh token.
     * Accepts refresh token from request body (mobile) or cookie (web).
     * Sets updated HttpOnly cookies for web clients.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = resolveRefreshToken(body, request);
        AuthResponse tokens = authService.refresh(new RefreshTokenRequest(refreshToken));
        addAuthCookies(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), tokens));
    }

    /**
     * Revoke the refresh token and clear auth cookies (web logout).
     * Mobile clients can ignore this endpoint and simply discard their tokens.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = extractCookieValue(request, REFRESH_TOKEN_COOKIE);
        if (refreshToken != null) {
            authService.revokeRefreshToken(refreshToken);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
    }

    /**
     * Returns the authenticated user's profile.
     * Web clients use this to bootstrap auth state after page load (avoids browser-side JWT decode).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(
            @AuthenticationPrincipal AppUserDetails principal) {
        MeResponse profile = new MeResponse(principal.getUserId(), principal.getPhone(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), profile));
    }

    // ── Cookie helpers ───────────────────────────────────────────────────────

    private void addAuthCookies(HttpServletResponse response, AuthResponse tokens) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, tokens.accessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(accessTokenMaxAge)
                .sameSite(cookieSameSite)
                .build();
        // Scope refresh cookie to refresh path to minimise exposure
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .maxAge(refreshTokenMaxAge)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private String resolveRefreshToken(RefreshTokenRequest body, HttpServletRequest request) {
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        String fromCookie = extractCookieValue(request, REFRESH_TOKEN_COOKIE);
        if (fromCookie != null) {
            return fromCookie;
        }
        throw new BusinessException("No refresh token provided");
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
