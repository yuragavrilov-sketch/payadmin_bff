package ru.copperside.payadmin.gos.application;

public record GosExtractCommand(String merchId, String beneficiaryType, String inn, String name) {
}
