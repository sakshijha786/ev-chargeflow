package com.evcharging.station.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChargerRequest {

    @NotBlank(message = "code is required")
    private String code;

    private String location;

    private Double powerRatingKw;
}
