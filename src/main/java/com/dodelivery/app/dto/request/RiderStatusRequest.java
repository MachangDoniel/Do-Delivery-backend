package com.dodelivery.app.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Updates the rider's online/offline status.
 * Maps to: POST /rider/status
 */
public record RiderStatusRequest(

        @NotNull(message = "Online flag is required")
        Boolean online
) {}
