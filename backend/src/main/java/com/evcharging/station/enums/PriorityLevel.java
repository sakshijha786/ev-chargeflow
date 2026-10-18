package com.evcharging.station.enums;

import lombok.Getter;

@Getter
public enum PriorityLevel {
    STANDARD(1, "Standard", "Normal queue turn"),
    PREMIUM(2, "Premium", "Priority queue & faster DC charging"),
    EMERGENCY(3, "Emergency", "Highest priority emergency preemption");

    private final int rank;
    private final String displayName;
    private final String description;

    PriorityLevel(int rank, String displayName, String description) {
        this.rank = rank;
        this.displayName = displayName;
        this.description = description;
    }
}
