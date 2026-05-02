package com.dodelivery.app.controller;

import com.dodelivery.app.dto.request.RiderStatusRequest;
import com.dodelivery.app.dto.request.UpdateLocationRequest;
import com.dodelivery.app.dto.response.ApiResponse;
import com.dodelivery.app.dto.response.OrderResponse;
import com.dodelivery.app.security.AppUserDetails;
import com.dodelivery.app.service.RiderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * All endpoints require RIDER role.
 */
@RestController
@RequestMapping({"/rider", "/api/riders"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('RIDER')")
public class RiderController {

    private final RiderService riderService;

    /**
     * List orders assigned to the authenticated rider.
     * GET /api/riders/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal AppUserDetails principal) {

        List<OrderResponse> orders = riderService.getMyOrders(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), orders));
    }

    /**
     * Quick online toggle endpoint.
     * POST /api/riders/online
     */
    @PostMapping("/online")
    public ResponseEntity<ApiResponse<Void>> goOnline(
            @AuthenticationPrincipal AppUserDetails principal) {

        riderService.goOnline(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
    }

    /**
     * Quick offline toggle endpoint.
     * POST /api/riders/offline
     */
    @PostMapping("/offline")
    public ResponseEntity<ApiResponse<Void>> goOffline(
            @AuthenticationPrincipal AppUserDetails principal) {

        riderService.goOffline(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
    }

    /**
     * Set rider online/offline status.
     * POST /rider/status  { "online": true }
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @Valid @RequestBody RiderStatusRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        if (Boolean.TRUE.equals(request.online())) {
            riderService.goOnline(principal.getUserId());
        } else {
            riderService.goOffline(principal.getUserId());
        }
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
    }

    /**
     * Push current GPS coordinates to Redis (real-time tracking).
     * POST /rider/location  { "lat": 1.23, "lng": 4.56 }
     */
    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody UpdateLocationRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {

        riderService.updateLocation(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), null));
    }

    /**
     * Accept a delivery order.
     * POST /rider/order/{id}/accept
     */
    @PostMapping("/order/{id}/accept")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = riderService.acceptOrder(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * Reject an assigned order.
     * POST /rider/order/{id}/reject
     */
    @PostMapping("/order/{id}/reject")
    public ResponseEntity<ApiResponse<OrderResponse>> rejectOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = riderService.rejectOrder(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * Mark item as picked up from sender.
     * POST /rider/order/{id}/pickup
     */
    @PostMapping("/order/{id}/pickup")
    public ResponseEntity<ApiResponse<OrderResponse>> markPickedUp(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = riderService.markPickedUp(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    /**
     * Mark order as delivered to recipient.
     * POST /rider/order/{id}/deliver
     */
    @PostMapping("/order/{id}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> markDelivered(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails principal) {

        OrderResponse response = riderService.markDelivered(id, principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }
}
