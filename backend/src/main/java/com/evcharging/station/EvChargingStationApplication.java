package com.evcharging.station;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. @EnableScheduling powers the two background jobs that make the
 * "5-minute grace period + waiting queue" behaviour automatic:
 *   - BookingExpiryScheduler: cancels bookings whose grace period passed and
 *     reassigns the freed charger to the next queued user.
 *   - ReminderScheduler: emails users shortly before their deadline expires.
 */
@SpringBootApplication
@EnableScheduling
public class EvChargingStationApplication {
    public static void main(String[] args) {
        SpringApplication.run(EvChargingStationApplication.class, args);
    }
}
