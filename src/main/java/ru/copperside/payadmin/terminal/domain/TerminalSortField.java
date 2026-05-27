package ru.copperside.payadmin.terminal.domain;

import java.util.Locale;

public enum TerminalSortField {
    MERC_ID("mercId"),
    MPS("mps"),
    GATE("gate"),
    TERMINAL_ID("terminalId"),
    MCC("mcc");

    private final String value;

    TerminalSortField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static TerminalSortField from(String value) {
        if (value == null || value.isBlank()) {
            return MERC_ID;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (TerminalSortField field : values()) {
            if (field.value.toLowerCase(Locale.ROOT).equals(normalized)) {
                return field;
            }
        }
        throw new IllegalArgumentException("Unsupported sortBy: " + value);
    }
}
