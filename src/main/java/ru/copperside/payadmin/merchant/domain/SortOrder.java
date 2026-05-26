package ru.copperside.payadmin.merchant.domain;

public record SortOrder<T>(
        T field,
        SortDirection direction
) {
    public static <T> SortOrder<T> of(T field, SortDirection direction) {
        return new SortOrder<>(field, direction == null ? SortDirection.ASC : direction);
    }
}

