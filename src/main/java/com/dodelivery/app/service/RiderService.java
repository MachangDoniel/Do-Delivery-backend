package com.dodelivery.app.service;

import com.dodelivery.app.dto.request.UpdateLocationRequest;
import com.dodelivery.app.dto.response.OrderResponse;

import java.util.UUID;

public interface RiderService {

    /** Mark rider as online. */
    void goOnline(UUID riderId);

    /** Mark rider as offline and clear Redis location entry. */
    void goOffline(UUID riderId);

    /** Store rider's current GPS coordinates in Redis and broadcast via WebSocket. */
    void updateLocation(UUID riderId, UpdateLocationRequest request);

    /** Accept an order (status: CREATED → ACCEPTED). */
    OrderResponse acceptOrder(UUID orderId, UUID riderId);

    /** Mark item as picked up (status: ACCEPTED → PICKED). */
    OrderResponse markPickedUp(UUID orderId, UUID riderId);

    /** Mark order as delivered (status: PICKED → DELIVERED). */
    OrderResponse markDelivered(UUID orderId, UUID riderId);
}
