package ru.copperside.payadmin.limit.application;

public record PatchGroupTypeCommand(String name, String description, Boolean enabled, Integer sortOrder) {
}
