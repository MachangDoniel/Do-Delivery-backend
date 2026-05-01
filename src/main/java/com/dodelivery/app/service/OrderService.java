package com.dodelivery.app.service;

import com.dodelivery.app.dto.request.CreateOrderRequest;
import com.dodelivery.app.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    /** Place a new delivery order (CUSTOMER only). */
    OrderResponse createOrder(CreateOrderRequest request, UUID customerId);

    /** Retrieve order details (accessible to the assigned customer or rider). */
    OrderResponse getOrder(UUID orderId);

    /** Retrieve all orders for a customer. */
    List<OrderResponse> getCustomerOrders(UUID customerId);

    /** Cancel an order (only allowed when status is CREATED). */
    OrderResponse cancelOrder(UUID orderId, UUID customerId);
}
