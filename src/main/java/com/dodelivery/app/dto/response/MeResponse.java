package com.dodelivery.app.dto.response;

import com.dodelivery.app.enums.Role;

import java.util.UUID;

/**
 * Lightweight profile payload returned by GET /api/auth/me.
 * Used by web clients to bootstrap auth state without decoding a JWT in the browser.
 */
public record MeResponse(UUID userId, String phone, Role role) {}
