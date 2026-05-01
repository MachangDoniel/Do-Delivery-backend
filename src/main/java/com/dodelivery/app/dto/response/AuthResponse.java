package com.dodelivery.app.dto.response;

/**
 * Returned after successful OTP verification (login).
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn      // access token TTL in seconds
) {}
