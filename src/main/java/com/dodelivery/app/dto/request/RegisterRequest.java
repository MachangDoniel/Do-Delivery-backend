package com.dodelivery.app.dto.request;

import com.dodelivery.app.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registers a new user.  On success the caller receives an OTP (mock in dev)
 * that must be exchanged via POST /auth/login to obtain JWT tokens.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
        String phone,

        @NotNull(message = "Role is required")
        Role role
) {}
