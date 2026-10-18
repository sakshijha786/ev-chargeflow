package com.evcharging.station.dto;

import com.evcharging.station.enums.PriorityLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkInRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    private String customerPhone;

    private String vehicleModel;

    private Double energyKwh;

    @Builder.Default
    private PriorityLevel priority = PriorityLevel.STANDARD;
}
