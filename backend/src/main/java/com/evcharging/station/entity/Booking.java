package com.evcharging.station.entity;

import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.enums.PriorityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_status_deadline", columnList = "status, arrivalDeadline")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PriorityLevel priority = PriorityLevel.STANDARD;

    private Double energyTargetKwh;

    private Double energyDeliveredKwh;

    @Builder.Default
    private Boolean isWalkIn = false;

    private String customerName;

    private LocalDateTime bookingTime;

    /** bookingTime + grace period (default 5 minutes). */
    private LocalDateTime arrivalDeadline;

    private LocalDateTime arrivedAt;

    private LocalDateTime chargingStartedAt;

    private LocalDateTime chargingEndedAt;

    @Builder.Default
    private Boolean reminderSent = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.reminderSent == null) {
            this.reminderSent = false;
        }
        if (this.priority == null) {
            this.priority = PriorityLevel.STANDARD;
        }
        if (this.isWalkIn == null) {
            this.isWalkIn = false;
        }
        if (this.energyDeliveredKwh == null) {
            this.energyDeliveredKwh = 0.0;
        }
        if (this.customerName == null && this.user != null) {
            this.customerName = this.user.getName();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
