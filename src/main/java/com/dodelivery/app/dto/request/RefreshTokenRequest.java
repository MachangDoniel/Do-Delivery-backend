package com.dodelivery.app.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Rotates an expired access token using a valid refresh token.
 * Maps to: POST /auth/refresh
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
