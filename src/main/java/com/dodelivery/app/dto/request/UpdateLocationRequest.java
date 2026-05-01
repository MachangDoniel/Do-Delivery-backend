package com.dodelivery.app.dto.request;

import jakarta.validation.constraints.*;

/**
 * Rider live-location update.  Stored in Redis with a TTL.
 * Maps to: POST /rider/location
 */
public record UpdateLocationRequest(

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
        Double lat,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        Double lng
) {}
