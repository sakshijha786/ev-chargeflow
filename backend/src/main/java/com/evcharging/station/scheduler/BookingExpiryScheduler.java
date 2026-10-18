package com.evcharging.station.scheduler;

import com.evcharging.station.entity.Booking;
import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.repository.BookingRepository;
import com.evcharging.station.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls for bookings whose 5-minute arrival grace period has passed with no
 * arrival, and for each one: marks it EXPIRED, emails the user, frees the
 * charger, and allocates it to the first user in the waiting queue (if any).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Scheduled(fixedRateString = "${ev.scheduler.expiry-check-rate-ms:30000}")
    public void expireOverdueBookings() {
        List<Booking> overdue = bookingRepository.findByStatusAndArrivalDeadlineBefore(
                BookingStatus.PENDING_ARRIVAL, LocalDateTime.now());

        if (overdue.isEmpty()) {
            return;
        }

        log.info("Found {} overdue booking(s); expiring and processing waiting queue", overdue.size());

        for (Booking booking : overdue) {
            try {
                bookingService.expireBooking(booking);
            } catch (Exception e) {
                log.error("Failed to expire booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
