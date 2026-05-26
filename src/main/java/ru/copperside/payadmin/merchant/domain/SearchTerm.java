package ru.copperside.payadmin.merchant.domain;

import java.util.Locale;

public record SearchTerm(String value) {

    public static SearchTerm of(String value) {
        if (value == null || value.isBlank()) {
            return empty();
        }
        return new SearchTerm(value.trim().toLowerCase(Locale.ROOT));
    }

    public static SearchTerm empty() {
        return new SearchTerm(null);
    }

    public boolean isPresent() {
        return value != null && !value.isBlank();
    }
}

