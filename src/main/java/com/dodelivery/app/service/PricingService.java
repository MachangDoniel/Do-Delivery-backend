package com.dodelivery.app.service;

import java.math.BigDecimal;

public interface PricingService {

    /**
     * Calculates the delivery price based on straight-line distance
     * between pickup and drop coordinates.
     *
     * <pre>price = baseFare + (distance_km * perKmRate)</pre>
     *
     * @return price rounded to 2 decimal places
     */
    BigDecimal calculatePrice(double pickupLat, double pickupLng,
                              double dropLat,   double dropLng);
}
