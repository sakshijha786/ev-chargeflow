package com.evcharging.station.repository;

import com.evcharging.station.entity.Booking;
import com.evcharging.station.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatus(BookingStatus status);

    /** Bookings still awaiting arrival whose deadline has already passed -> expire them. */
    List<Booking> findByStatusAndArrivalDeadlineBefore(BookingStatus status, LocalDateTime time);

    /** Bookings awaiting arrival, not yet reminded, whose deadline is within the reminder window. */
    List<Booking> findByStatusAndReminderSentFalseAndArrivalDeadlineBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByChargerIdAndStatus(Long chargerId, BookingStatus status);
}
