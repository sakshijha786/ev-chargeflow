package com.evcharging.station.service;

import com.evcharging.station.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    public void sendBookingConfirmation(Booking booking) {
        String subject = "Booking Confirmed - Charger " + booking.getCharger().getCode();
        String body = String.format(
                "Hi %s,%n%n" +
                "Your charger booking is confirmed.%n%n" +
                "Charger: %s%n" +
                "Booking Time: %s%n" +
                "Arrive by: %s%n%n" +
                "You have a 5-minute grace period to arrive. If you don't arrive by the deadline above, " +
                "your booking will be automatically cancelled and the charger given to the next person in the waiting queue.%n%n" +
                "Thanks,%nEV Charging Station",
                booking.getUser().getName(),
                booking.getCharger().getCode(),
                booking.getBookingTime().format(FORMATTER),
                booking.getArrivalDeadline().format(FORMATTER)
        );
        send(booking.getUser().getEmail(), subject, body);
    }

    public void sendArrivalReminder(Booking booking) {
        String subject = "Reminder: Arrive soon for Charger " + booking.getCharger().getCode();
        String body = String.format(
                "Hi %s,%n%n" +
                "This is a reminder that you must arrive at charger %s by %s, or your booking will be " +
                "automatically cancelled and reassigned to the next person waiting.%n%n" +
                "Thanks,%nEV Charging Station",
                booking.getUser().getName(),
                booking.getCharger().getCode(),
                booking.getArrivalDeadline().format(FORMATTER)
        );
        send(booking.getUser().getEmail(), subject, body);
    }

    public void sendBookingExpired(Booking booking) {
        String subject = "Booking Cancelled - Charger " + booking.getCharger().getCode();
        String body = String.format(
                "Hi %s,%n%n" +
                "You did not arrive within the 5-minute grace period, so your booking for charger %s has been " +
                "automatically cancelled. The charger has been offered to the next person in the waiting queue.%n%n" +
                "Thanks,%nEV Charging Station",
                booking.getUser().getName(),
                booking.getCharger().getCode()
        );
        send(booking.getUser().getEmail(), subject, body);
    }

    public void sendQueueAllocation(Booking booking) {
        String subject = "Charger Now Available - " + booking.getCharger().getCode();
        String body = String.format(
                "Hi %s,%n%n" +
                "A charger has become available and has been allocated to you from the waiting queue.%n%n" +
                "Charger: %s%n" +
                "Please arrive by %s or the booking will be cancelled and passed to the next person waiting.%n%n" +
                "Thanks,%nEV Charging Station",
                booking.getUser().getName(),
                booking.getCharger().getCode(),
                booking.getArrivalDeadline().format(FORMATTER)
        );
        send(booking.getUser().getEmail(), subject, body);
    }

    public void sendQueueJoinedNotification(String email, String name, int position) {
        String subject = "Added to Waiting Queue";
        String body = String.format(
                "Hi %s,%n%n" +
                "All chargers are currently occupied. You have been added to the waiting queue at position %d. " +
                "You'll receive an email as soon as a charger is allocated to you.%n%n" +
                "Thanks,%nEV Charging Station",
                name, position
        );
        send(email, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Never let a mail-server hiccup break the booking flow itself.
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
