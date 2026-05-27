package ru.copperside.payadmin.limit.application;

public record PatchGroupCommand(String name, String description, Boolean enabled) {
}
