package com.dodelivery.app.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Rider live location snapshot published over WebSocket and returned from
 * the LocationService. Stored in Redis as JSON.
 */
public record RiderLocationResponse(
        UUID riderId,
        double lat,
        double lng,
        Instant timestamp
) {}
