package com.evcharging.station.dto;

import com.evcharging.station.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    private Long userId;

    private String vehicleModel;

    private Double currentBatteryPercent;

    private Double targetBatteryPercent;

    private Double energyKwh;

    @Builder.Default
    private PriorityLevel priority = PriorityLevel.STANDARD;

    private Boolean isWalkIn;

    private String customerName;

    private String customerPhone;
}
