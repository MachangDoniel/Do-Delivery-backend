package com.dodelivery.app.service.impl;

import com.dodelivery.app.dto.request.CreateOrderRequest;
import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.entity.Order;
import com.dodelivery.app.entity.User;
import com.dodelivery.app.enums.OrderStatus;
import com.dodelivery.app.exception.BusinessException;
import com.dodelivery.app.exception.ResourceNotFoundException;
import com.dodelivery.app.repository.OrderRepository;
import com.dodelivery.app.repository.UserRepository;
import com.dodelivery.app.service.OrderService;
import com.dodelivery.app.service.PricingService;
import com.dodelivery.app.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository        orderRepository;
    private final UserRepository         userRepository;
    private final PricingService         pricingService;
    private final WebSocketEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        BigDecimal price = pricingService.calculatePrice(
                request.pickupLat(), request.pickupLng(),
                request.dropLat(),   request.dropLng());

        Order order = Order.builder()
                .customer(customer)
                .pickupLat(request.pickupLat())
                .pickupLng(request.pickupLng())
                .dropLat(request.dropLat())
                .dropLng(request.dropLng())
                .type(request.type())
                .price(price)
                .note(request.note())
                .build();

        Order saved = orderRepository.save(order);
        OrderResponse response = toResponse(saved);

        // Notify online riders of the new delivery request
        eventPublisher.publishNewOrderRequest(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return toResponse(findOrderWithDetails(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(UUID customerId) {
        return orderRepository.findAllByCustomerIdWithDetails(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID customerId) {
        Order order = findOrderWithDetails(orderId);

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You can only cancel your own orders");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException(
                    "Order can only be cancelled when status is CREATED. Current status: "
                    + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        OrderResponse response = toResponse(saved);

        eventPublisher.publishOrderStatusUpdate(orderId, response);
        return response;
    }

    // ── Package-level helper used by RiderServiceImpl ────────────────────────

    Order findOrderWithDetails(UUID orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    /** Maps Order entity → OrderResponse DTO. Never returns entity references directly. */
    OrderResponse toResponse(Order order) {
        User rider = order.getRider();
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                rider != null ? rider.getId()   : null,
                rider != null ? rider.getName() : null,
                order.getPickupLat(),
                order.getPickupLng(),
                order.getDropLat(),
                order.getDropLng(),
                order.getType(),
                order.getStatus(),
                order.getPrice(),
                order.getNote(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
