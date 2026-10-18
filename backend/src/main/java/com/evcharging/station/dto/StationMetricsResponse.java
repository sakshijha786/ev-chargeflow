package com.evcharging.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationMetricsResponse {

    private long totalChargers;
    private long availableChargers;
    private long activeChargingSessions;
    private long pendingArrivals;
    private long outOfServiceChargers;
    private long queueDepth;
    private double totalRevenueInr;
    private double totalEnergyDeliveredKwh;
    private double totalGstCollectedInr;
    private long completedSessions;
}
