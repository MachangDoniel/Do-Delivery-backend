package com.dodelivery.app.service;

import com.dodelivery.app.dto.request.UpdateLocationRequest;
import com.dodelivery.app.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface RiderService {

    /** List all orders assigned to the authenticated rider. */
    List<OrderResponse> getMyOrders(UUID riderId);

    /** Assign nearest available rider to the order (status: CREATED → ASSIGNED). */
    OrderResponse assignNearestRider(UUID orderId, UUID customerId);

    /** Attempt to auto-assign nearest available rider to an order (returns null if no rider found). */
    OrderResponse tryAutoAssignRider(UUID orderId);

    /** Mark rider as online. */
    void goOnline(UUID riderId);

    /** Mark rider as offline and clear Redis location entry. */
    void goOffline(UUID riderId);

    /** Store rider's current GPS coordinates in Redis and broadcast via WebSocket. */
    void updateLocation(UUID riderId, UpdateLocationRequest request);

    /** Accept an order (status: ASSIGNED → ACCEPTED). */
    OrderResponse acceptOrder(UUID orderId, UUID riderId);

    /** Reject an assigned order (status: ASSIGNED → CREATED and unassign rider). */
    OrderResponse rejectOrder(UUID orderId, UUID riderId);

    /** Mark item as picked up (status: ACCEPTED → PICKED_UP). */
    OrderResponse markPickedUp(UUID orderId, UUID riderId);

    /** Mark order as delivered (status: PICKED_UP → DELIVERED). */
    OrderResponse markDelivered(UUID orderId, UUID riderId);
}
