package com.dodelivery.app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemStatsResponse {
    private long totalUsers;
    private long totalOrders;
    private long activeOrders;
    private long totalRiders;
    private long onlineRiders;
    private double totalRevenue;
}
