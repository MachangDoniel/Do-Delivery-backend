package com.dodelivery.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Verifies the OTP sent to the user's phone and returns JWT tokens.
 * Maps to: POST /auth/login
 */
public record LoginRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "OTP is required")
        @Size(min = 4, max = 6, message = "OTP must be 4–6 digits")
        @Pattern(regexp = "\\d+", message = "OTP must contain digits only")
        String otp
) {}
