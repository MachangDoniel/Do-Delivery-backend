package com.dodelivery.app.controller;

import com.dodelivery.app.dto.request.CreateOrderRequest;
import com.dodelivery.app.dto.response.ApiResponse;
import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.security.AppUserDetails;
import com.dodelivery.app.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/orders", "/api/orders"})
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new delivery order.
     * Only CUSTOMER role is allowed.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = orderService.createOrder(request, principal.getUserId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), response));
    }

    /**
     * Get a specific order by ID.
     * Accessible to both customers and riders (server does not restrict by ownership here —
     * ownership validation can be layered in if needed).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * List all orders placed by the authenticated customer.
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal AppUserDetails principal) {

        List<OrderResponse> orders = orderService.getCustomerOrders(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), orders));
    }

    /**
     * Cancel an order.  Only allowed when status is CREATED.
     * Only the customer who placed the order can cancel it.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = orderService.cancelOrder(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }
}
