package com.dodelivery.app.enums;

public enum OrderStatus {
    CREATED,    // Customer placed the order, waiting for a rider
    ACCEPTED,   // Rider accepted the order
    PICKED,     // Rider picked up the item
    DELIVERED,  // Rider delivered the item
    CANCELLED   // Order cancelled by customer (only allowed in CREATED state)
}
