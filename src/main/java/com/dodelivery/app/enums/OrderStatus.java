package com.dodelivery.app.enums;

public enum OrderStatus {
    CREATED,    // Customer placed the order, waiting for dispatch
    ASSIGNED,   // System assigned the nearest available rider
    ACCEPTED,   // Assigned rider accepted the order
    PICKED_UP,  // Rider picked up the item
    DELIVERED,  // Rider delivered the item
    CANCELLED   // Order cancelled by customer (only allowed in CREATED state)
}
