package ru.copperside.payadmin.crossborder.domain;

public record ProviderRequiredField(
        String name,
        String type,
        String description,
        boolean required
) {
}
