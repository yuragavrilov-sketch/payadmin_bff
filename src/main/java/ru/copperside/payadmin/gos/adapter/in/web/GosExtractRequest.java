package ru.copperside.payadmin.gos.adapter.in.web;

public record GosExtractRequest(String merchId, String beneficiaryType, String inn, String name) {
}
