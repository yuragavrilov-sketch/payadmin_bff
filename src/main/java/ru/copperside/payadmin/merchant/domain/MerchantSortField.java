package ru.copperside.payadmin.merchant.domain;

import java.util.Locale;

public enum MerchantSortField {
    ID("id"),
    NAME("name"),
    STATUS("status"),
    MCC("mcc"),
    CREATED_AT("createdAt");

    private final String value;

    MerchantSortField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MerchantSortField from(String value) {
        if (value == null || value.isBlank()) {
            return ID;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MerchantSortField field : values()) {
            if (field.value.toLowerCase(Locale.ROOT).equals(normalized)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unsupported sortBy: " + value);
    }
}

