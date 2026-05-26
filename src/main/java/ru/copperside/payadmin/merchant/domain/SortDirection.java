package ru.copperside.payadmin.merchant.domain;

import java.util.Locale;

public enum SortDirection {
    ASC("asc"),
    DESC("desc");

    private final String value;

    SortDirection(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (SortDirection direction : values()) {
            if (direction.value.equals(normalized)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Unsupported sortDir: " + value);
    }
}

