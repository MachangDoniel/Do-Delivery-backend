package com.dodelivery.app.service.impl;

import com.dodelivery.app.dto.request.UpdateLocationRequest;
import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.entity.Order;
import com.dodelivery.app.entity.User;
import com.dodelivery.app.enums.OrderStatus;
import com.dodelivery.app.exception.BusinessException;
import com.dodelivery.app.exception.ResourceNotFoundException;
import com.dodelivery.app.repository.OrderRepository;
import com.dodelivery.app.repository.RiderProfileRepository;
import com.dodelivery.app.repository.UserRepository;
import com.dodelivery.app.service.LocationService;
import com.dodelivery.app.service.RiderService;
import com.dodelivery.app.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiderServiceImpl implements RiderService {

    private final RiderProfileRepository riderProfileRepository;
    private final OrderRepository        orderRepository;
    private final UserRepository         userRepository;
    private final LocationService        locationService;
    private final WebSocketEventPublisher eventPublisher;
    private final OrderServiceImpl       orderServiceImpl;   // reuse mapping logic

    // ── Online / Offline ─────────────────────────────────────────────────────

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
        // Verify the rider exists before persisting location to Redis
        if (!riderProfileRepository.existsByUserId(riderId)) {
            throw new ResourceNotFoundException("Rider profile not found");
        }
        locationService.saveRiderLocation(riderId, request.lat(), request.lng());
        eventPublisher.publishRiderLocationUpdate(riderId, request.lat(), request.lng());
    }

    // ── Order flow ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse acceptOrder(UUID orderId, UUID riderId) {
        Order order = findOrderForRider(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Order is no longer available. Status: " + order.getStatus());
        }
        if (!isRiderOnline(riderId)) {
            throw new BusinessException("Rider must be online to accept orders");
        }

        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found"));

        order.setRider(rider);
        order.setStatus(OrderStatus.ACCEPTED);
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

        order.setStatus(OrderStatus.PICKED);
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
        assertOrderStatus(order, OrderStatus.PICKED, "delivery");

        order.setStatus(OrderStatus.DELIVERED);
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
}
