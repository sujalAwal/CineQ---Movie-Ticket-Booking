package com.awal.cineq.publicapi.showtime.dto;

/**
 * Enum representing customer's seat position preference
 * Influences how the seat suggestion algorithm scores rows
 */
public enum SeatPreference {
    FRONT("FRONT", "Prefer front rows for closer screen view"),
    MIDDLE("MIDDLE", "Prefer middle rows for optimal viewing angle (default)"),
    BACK("BACK", "Prefer back rows for more space and comfort");

    private final String code;
    private final String description;

    SeatPreference(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
