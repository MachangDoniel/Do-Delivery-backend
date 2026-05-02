package com.dodelivery.app.controller;

import com.dodelivery.app.dto.response.*;
import com.dodelivery.app.entity.Order;
import com.dodelivery.app.entity.SystemSettings;
import com.dodelivery.app.entity.User;
import com.dodelivery.app.enums.OrderStatus;
import com.dodelivery.app.enums.Role;
import com.dodelivery.app.repository.OrderRepository;
import com.dodelivery.app.repository.RiderProfileRepository;
import com.dodelivery.app.repository.SystemSettingsRepository;
import com.dodelivery.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final SystemSettingsRepository settingsRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStatsResponse>> getStats() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long activeOrders = orderRepository.findAllByStatus(OrderStatus.ASSIGNED).size() +
                orderRepository.findAllByStatus(OrderStatus.ACCEPTED).size() +
                orderRepository.findAllByStatus(OrderStatus.PICKED_UP).size();
        long totalRiders = riderProfileRepository.count();
        long onlineRiders = riderProfileRepository.countByOnlineTrue();

        SystemStatsResponse stats = SystemStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalOrders(totalOrders)
                .activeOrders(activeOrders)
                .totalRiders(totalRiders)
                .onlineRiders(onlineRiders)
                .totalRevenue(0.0) // Placeholder
                .build();

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getName(), u.getPhone(), u.getRole(), u.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), users));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Role newRole = Role.valueOf(body.get("role"));
        user.setRole(newRole);
        userRepository.save(user);

        UserResponse response = new UserResponse(user.getId(), user.getName(), user.getPhone(), user.getRole(), user.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), response));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        List<OrderResponse> orders = orderRepository.findAllWithDetails().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), orders));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSettings>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), settingsRepository.findAll()));
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<SystemSettings>> updateSetting(@RequestBody SystemSettings setting) {
        SystemSettings saved = settingsRepository.save(setting);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), saved));
    }

    private OrderResponse mapToOrderResponse(Order o) {
        return new OrderResponse(
                o.getId(),
                o.getCustomer().getId(),
                o.getCustomer().getName(),
                o.getRider() != null ? o.getRider().getId() : null,
                o.getRider() != null ? o.getRider().getName() : null,
                o.getPickupLat(),
                o.getPickupLng(),
                o.getDropLat(),
                o.getDropLng(),
                o.getType(),
                o.getStatus(),
                o.getPrice(),
                o.getNote(),
                o.getAssignedAt(),
                o.getPickedUpAt(),
                o.getDeliveredAt(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
