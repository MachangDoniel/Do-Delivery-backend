package com.dodelivery.app.dto.request;

import com.dodelivery.app.enums.OrderType;
import jakarta.validation.constraints.*;

/**
 * Payload for creating a new delivery order.
 * Price is calculated server-side via the PricingService.
 */
public record CreateOrderRequest(

        @NotNull(message = "Pickup latitude is required")
        @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
        Double pickupLat,

        @NotNull(message = "Pickup longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        Double pickupLng,

        @NotNull(message = "Drop latitude is required")
        @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
        @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
        Double dropLat,

        @NotNull(message = "Drop longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
        Double dropLng,

        @NotNull(message = "Order type is required")
        OrderType type,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
