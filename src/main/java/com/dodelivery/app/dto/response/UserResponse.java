package com.dodelivery.app.dto.response;

import com.dodelivery.app.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String phone,
        Role role,
        Instant createdAt
) {}
