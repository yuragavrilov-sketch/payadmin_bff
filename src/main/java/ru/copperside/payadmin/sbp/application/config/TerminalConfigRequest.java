package ru.copperside.payadmin.sbp.application.config;

public record TerminalConfigRequest(String c2bFieldName, String b2cFieldName, String tkbPayPrefix) {
}
