package ru.copperside.payadmin.merchant.domain;

public record PageWindow(
        int limit,
        int offset
) {
    public static final int MAX_LIMIT = 500;

    public static PageWindow of(int limit, int offset) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be greater than or equal to 0");
        }
        return new PageWindow(limit, offset);
    }
}

