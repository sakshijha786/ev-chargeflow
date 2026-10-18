package com.evcharging.station.scheduler;

import com.evcharging.station.entity.Booking;
import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.repository.BookingRepository;
import com.evcharging.station.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Emails a reminder to users who are within {@code reminderBeforeMinutes} of
 * their arrival deadline and haven't been reminded yet.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Value("${ev.booking.reminder-before-minutes:2}")
    private int reminderBeforeMinutes;

    @Scheduled(fixedRateString = "${ev.scheduler.reminder-check-rate-ms:30000}")
    public void sendArrivalReminders() {
        LocalDateTime threshold = LocalDateTime.now().plusMinutes(reminderBeforeMinutes);

        List<Booking> dueForReminder = bookingRepository
                .findByStatusAndReminderSentFalseAndArrivalDeadlineBefore(BookingStatus.PENDING_ARRIVAL, threshold);

        for (Booking booking : dueForReminder) {
            try {
                emailService.sendArrivalReminder(booking);
                booking.setReminderSent(true);
                bookingRepository.save(booking);
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }
}
