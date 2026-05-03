package com.dodelivery.app.repository;

import com.dodelivery.app.entity.Order;
import com.dodelivery.app.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Fetch a single order with customer and rider eagerly loaded
     * to avoid N+1 when building OrderResponse.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.rider " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    /**
     * All orders for a specific customer, newest first.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.rider " +
           "WHERE o.customer.id = :customerId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllByCustomerIdWithDetails(@Param("customerId") UUID customerId);

    /**
     * All orders currently assigned to a rider.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.customer " +
           "JOIN FETCH o.rider " +
           "WHERE o.rider.id = :riderId " +
           "ORDER BY CASE " +
           "WHEN o.status = com.dodelivery.app.enums.OrderStatus.ASSIGNED THEN 0 " +
           "WHEN o.status = com.dodelivery.app.enums.OrderStatus.ACCEPTED THEN 1 " +
           "WHEN o.status = com.dodelivery.app.enums.OrderStatus.PICKED_UP THEN 2 " +
           "WHEN o.status = com.dodelivery.app.enums.OrderStatus.DELIVERED THEN 3 " +
           "ELSE 4 END, " +
           "COALESCE(o.assignedAt, o.updatedAt, o.createdAt) DESC")
    List<Order> findAllByRiderIdWithDetails(@Param("riderId") UUID riderId);

    /**
     * All orders in the system with customer and rider details.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.rider " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllWithDetails();

    List<Order> findAllByStatus(OrderStatus status);
}
