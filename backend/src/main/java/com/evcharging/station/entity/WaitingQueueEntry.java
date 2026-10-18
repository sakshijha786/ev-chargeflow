package com.evcharging.station.entity;

import com.evcharging.station.enums.PriorityLevel;
import com.evcharging.station.enums.QueueStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A user or walk-in customer waiting for a charger to free up.
 * Ordered by Priority (EMERGENCY > PREMIUM > STANDARD), then requestedAt (FIFO).
 * When a charger becomes free and a WAITING entry exists, it is allocated
 * a booking on that charger.
 */
@Entity
@Table(name = "waiting_queue", indexes = {
    @Index(name = "idx_queue_status_pri_time", columnList = "status, priority, requestedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PriorityLevel priority = PriorityLevel.STANDARD;

    private Double energyKwhNeeded;

    @Builder.Default
    private Boolean isWalkIn = false;

    private String customerName;

    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status;

    private LocalDateTime requestedAt;

    private LocalDateTime allocatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = QueueStatus.WAITING;
        }
        if (this.priority == null) {
            this.priority = PriorityLevel.STANDARD;
        }
        if (this.isWalkIn == null) {
            this.isWalkIn = false;
        }
        if (this.customerName == null && this.user != null) {
            this.customerName = this.user.getName();
        }
    }
}
