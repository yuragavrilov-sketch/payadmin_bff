package ru.copperside.payadmin.limit.application;

public record CreateGroupTypeCommand(String code, String name, String description, int sortOrder) {
}
