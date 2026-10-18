package com.evcharging.station.enums;

public enum BookingStatus {
    /** Charger reserved; user has 5 minutes to arrive. */
    PENDING_ARRIVAL,
    /** User arrived in time; charger is actively charging their vehicle. */
    ACTIVE,
    /** Charging session finished normally. */
    COMPLETED,
    /** Cancelled by the user before arriving. */
    CANCELLED,
    /** Grace period passed with no arrival; auto-cancelled by the scheduler. */
    EXPIRED
}
