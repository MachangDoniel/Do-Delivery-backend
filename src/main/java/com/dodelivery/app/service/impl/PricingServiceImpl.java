package com.dodelivery.app.service.impl;

import com.dodelivery.app.service.PricingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates delivery price using the Haversine formula for distance.
 *
 * <pre>price = baseFare + (distance_km * perKmRate)</pre>
 */
@Service
public class PricingServiceImpl implements PricingService {

    @Value("${app.pricing.base-fare}")
    private BigDecimal baseFare;

    @Value("${app.pricing.per-km-rate}")
    private BigDecimal perKmRate;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public BigDecimal calculatePrice(double pickupLat, double pickupLng,
                                     double dropLat,   double dropLng) {
        double distanceKm = haversineDistance(pickupLat, pickupLng, dropLat, dropLng);
        BigDecimal distance = BigDecimal.valueOf(distanceKm);
        return baseFare.add(distance.multiply(perKmRate)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Haversine formula — returns great-circle distance in kilometres.
     * Accuracy is sufficient for delivery route pricing (±0.5% for short distances).
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
