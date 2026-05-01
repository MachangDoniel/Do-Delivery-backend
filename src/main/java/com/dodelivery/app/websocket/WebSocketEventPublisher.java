package com.dodelivery.app.websocket;

import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.dto.response.RiderLocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Pushes server-initiated events to STOMP topic destinations.
 *
 * <h3>Topic map:</h3>
 * <pre>
 * /topic/orders/new              — broadcast to all riders when a new order is created
 * /topic/orders/{orderId}        — order status change (customer + assigned rider)
 * /topic/riders/{riderId}/location — live GPS update (relevant customer watches this)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a new order to all online riders.
     * Riders subscribe to {@code /topic/orders/new}.
     */
    public void publishNewOrderRequest(OrderResponse order) {
        messagingTemplate.convertAndSend("/topic/orders/new", order);
        log.debug("Published new_order_request for orderId={}", order.id());
    }

    /**
     * Notify both customer and rider of an order status change.
     * Subscribers: {@code /topic/orders/{orderId}}
     */
    public void publishOrderStatusUpdate(UUID orderId, OrderResponse order) {
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, order);
        log.debug("Published order_status_update orderId={} status={}", orderId, order.status());
    }

    /**
     * Push a rider's current GPS coordinates to the customer tracking them.
     * Subscribers: {@code /topic/riders/{riderId}/location}
     */
    public void publishRiderLocationUpdate(UUID riderId, double lat, double lng) {
        RiderLocationResponse location = new RiderLocationResponse(riderId, lat, lng, Instant.now());
        messagingTemplate.convertAndSend("/topic/riders/" + riderId + "/location", location);
        log.debug("Published rider_location_update riderId={}", riderId);
    }
}
