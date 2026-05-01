package com.dodelivery.app.service;

import com.dodelivery.app.dto.response.RiderLocationResponse;

import java.util.Optional;
import java.util.UUID;

public interface LocationService {

    /** Store or refresh a rider's live coordinates in Redis. */
    void saveRiderLocation(UUID riderId, double lat, double lng);

    /** Retrieve last known location for a rider. */
    Optional<RiderLocationResponse> getRiderLocation(UUID riderId);

    /** Remove a rider's location entry (on going offline). */
    void removeRiderLocation(UUID riderId);
}
