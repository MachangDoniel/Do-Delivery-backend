package com.dodelivery.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Requests a fresh OTP for an existing user (re-login flow).
 * Maps to: POST /auth/send-otp
 */
public record SendOtpRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
        String phone
) {}
