package com.dodelivery.app.service.impl;

import com.dodelivery.app.dto.request.UpdateLocationRequest;
import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.entity.Order;
import com.dodelivery.app.entity.RiderProfile;
import com.dodelivery.app.enums.OrderStatus;
import com.dodelivery.app.exception.BusinessException;
import com.dodelivery.app.exception.ResourceNotFoundException;
import com.dodelivery.app.repository.OrderRepository;
import com.dodelivery.app.repository.RiderProfileRepository;
import com.dodelivery.app.service.LocationService;
import com.dodelivery.app.service.RiderService;
import com.dodelivery.app.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final RiderProfileRepository riderProfileRepository;
    private final OrderRepository        orderRepository;
    private final LocationService        locationService;
    private final WebSocketEventPublisher eventPublisher;
    private final OrderServiceImpl       orderServiceImpl;   // reuse mapping logic

    // ── Online / Offline ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID riderId) {
        return orderRepository.findAllByRiderIdWithDetails(riderId)
                .stream()
                .map(orderServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse assignNearestRider(UUID orderId, UUID customerId) {
        Order order = findOrderForRider(orderId);

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only assign your own orders");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Order can only be assigned from CREATED state. Current: "
                    + order.getStatus());
        }

        List<RiderProfile> onlineRiders = riderProfileRepository.findOnlineWithKnownLocation();
        if (onlineRiders.isEmpty()) {
            throw new BusinessException("No online riders available right now");
        }

        RiderProfile nearest = onlineRiders.stream()
                .min(Comparator.comparingDouble(rp -> haversineKm(
                        order.getPickupLat(),
                        order.getPickupLng(),
                        rp.getCurrentLat(),
                        rp.getCurrentLng())))
                .orElseThrow(() -> new BusinessException("No rider found for assignment"));

        order.setRider(nearest.getUser());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(Instant.now());
        order.setPickedUpAt(null);
        order.setDeliveredAt(null);

        Order saved = orderRepository.save(order);
        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse tryAutoAssignRider(UUID orderId) {
        Order order = findOrderForRider(orderId);

        // Only auto-assign orders in CREATED state
        if (order.getStatus() != OrderStatus.CREATED) {
            return null;
        }

        List<RiderProfile> onlineRiders = riderProfileRepository.findOnlineWithKnownLocation();
        if (onlineRiders.isEmpty()) {
            // No online riders available — keep order in CREATED state for retry later
            return null;
        }

        RiderProfile nearest = onlineRiders.stream()
                .min(Comparator.comparingDouble(rp -> haversineKm(
                        order.getPickupLat(),
                        order.getPickupLng(),
                        rp.getCurrentLat(),
                        rp.getCurrentLng())))
                .orElse(null);

        if (nearest == null) {
            return null;
        }

        order.setRider(nearest.getUser());
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedAt(Instant.now());
        order.setPickedUpAt(null);
        order.setDeliveredAt(null);

        Order saved = orderRepository.save(order);
        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    @Override
    @Transactional
    public void goOnline(UUID riderId) {
        var profile = riderProfileRepository.findByUserIdWithUser(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setOnline(true);
        riderProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void goOffline(UUID riderId) {
        var profile = riderProfileRepository.findByUserIdWithUser(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        profile.setOnline(false);
        riderProfileRepository.save(profile);
        locationService.removeRiderLocation(riderId);
    }

    // ── Location ─────────────────────────────────────────────────────────────

    @Override
    public void updateLocation(UUID riderId, UpdateLocationRequest request) {
        var profile = riderProfileRepository.findByUserIdWithUser(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));

        profile.setCurrentLat(request.lat());
        profile.setCurrentLng(request.lng());
        riderProfileRepository.save(profile);

        locationService.saveRiderLocation(riderId, request.lat(), request.lng());
        eventPublisher.publishRiderLocationUpdate(riderId, request.lat(), request.lng());
    }

    // ── Order flow ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse acceptOrder(UUID orderId, UUID riderId) {
        Order order = findOrderForRider(orderId);

        if (order.getStatus() != OrderStatus.ASSIGNED) {
            throw new BusinessException("Order cannot be accepted now. Status: " + order.getStatus());
        }
        if (!isRiderOnline(riderId)) {
            throw new BusinessException("Rider must be online to accept orders");
        }

        assertRiderOwnsOrder(order, riderId);

        order.setStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);

        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse rejectOrder(UUID orderId, UUID riderId) {
        Order order = findOrderForRider(orderId);

        assertOrderStatus(order, OrderStatus.ASSIGNED, "reject");
        assertRiderOwnsOrder(order, riderId);

        order.setRider(null);
        order.setStatus(OrderStatus.CREATED);
        order.setAssignedAt(null);

        Order saved = orderRepository.save(order);
        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse markPickedUp(UUID orderId, UUID riderId) {
        Order order = findOrderForRider(orderId);
        assertRiderOwnsOrder(order, riderId);
        assertOrderStatus(order, OrderStatus.ACCEPTED, "pickup");

        order.setStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(Instant.now());
        Order saved = orderRepository.save(order);

        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse markDelivered(UUID orderId, UUID riderId) {
        Order order = findOrderForRider(orderId);
        assertRiderOwnsOrder(order, riderId);
        assertOrderStatus(order, OrderStatus.PICKED_UP, "delivery");

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        Order saved = orderRepository.save(order);

        OrderResponse response = orderServiceImpl.toResponse(saved);
        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Order findOrderForRider(UUID orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private void assertRiderOwnsOrder(Order order, UUID riderId) {
        if (order.getRider() == null || !order.getRider().getId().equals(riderId)) {
            throw new BusinessException("This order is not assigned to you");
        }
    }

    private void assertOrderStatus(Order order, OrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw new BusinessException(
                    "Cannot perform " + action + ". Order status is " + order.getStatus()
                    + ". Expected: " + expected);
        }
    }

    private boolean isRiderOnline(UUID riderId) {
        return riderProfileRepository.findByUserIdWithUser(riderId)
                .map(p -> p.isOnline())
                .orElse(false);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
