package com.evcharging.station.dto;

import com.evcharging.station.entity.Booking;
import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long bookingId;
    private Long userId;
    private String userName;
    private Long chargerId;
    private String chargerCode;
    private Double powerRatingKw;
    private BookingStatus status;
    private PriorityLevel priority;
    private Double energyTargetKwh;
    private Double energyDeliveredKwh;
    private Boolean isWalkIn;
    private LocalDateTime bookingTime;
    private LocalDateTime arrivalDeadline;
    private LocalDateTime arrivedAt;
    private LocalDateTime chargingStartedAt;
    private LocalDateTime chargingEndedAt;

    public static BookingResponse fromEntity(Booking booking) {
        String name = booking.getCustomerName();
        Long uid = null;
        if (booking.getUser() != null) {
            uid = booking.getUser().getId();
            if (name == null || name.isBlank()) {
                name = booking.getUser().getName();
            }
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .userId(uid)
                .userName(name != null ? name : "Walk-in Guest")
                .chargerId(booking.getCharger().getId())
                .chargerCode(booking.getCharger().getCode())
                .powerRatingKw(booking.getCharger().getPowerRatingKw())
                .status(booking.getStatus())
                .priority(booking.getPriority())
                .energyTargetKwh(booking.getEnergyTargetKwh())
                .energyDeliveredKwh(booking.getEnergyDeliveredKwh())
                .isWalkIn(booking.getIsWalkIn())
                .bookingTime(booking.getBookingTime())
                .arrivalDeadline(booking.getArrivalDeadline())
                .arrivedAt(booking.getArrivedAt())
                .chargingStartedAt(booking.getChargingStartedAt())
                .chargingEndedAt(booking.getChargingEndedAt())
                .build();
    }
}
