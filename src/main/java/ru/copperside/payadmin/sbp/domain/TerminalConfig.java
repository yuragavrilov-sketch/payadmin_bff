package ru.copperside.payadmin.sbp.domain;

import java.util.UUID;

public record TerminalConfig(
        UUID id, String c2bFieldName, String b2cFieldName, String tkbPayPrefix, String status, int version) {
}
