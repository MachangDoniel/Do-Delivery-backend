package com.dodelivery.app.dto.response;

import com.dodelivery.app.enums.OrderStatus;
import com.dodelivery.app.enums.OrderType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,

        // Customer summary
        UUID customerId,
        String customerName,

        // Rider summary (null until accepted)
        UUID riderId,
        String riderName,

        double pickupLat,
        double pickupLng,
        double dropLat,
        double dropLng,

        OrderType type,
        OrderStatus status,
        BigDecimal price,
        String note,

        Instant createdAt,
        Instant updatedAt
) {}
