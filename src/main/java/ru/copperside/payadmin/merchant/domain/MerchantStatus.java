package ru.copperside.payadmin.merchant.domain;

import java.util.Locale;

public enum MerchantStatus {
    ACTIVE("active"),
    SUSPENDED("suspended"),
    BLOCKED("blocked");

    private final String value;

    MerchantStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MerchantStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MerchantStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported status: " + value);
    }
}

