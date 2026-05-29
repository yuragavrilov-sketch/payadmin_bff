package ru.copperside.payadmin.sbp.domain;

import java.util.List;
import java.util.UUID;

public record ExtractionRule(
        UUID id, String messageType, List<FieldBinding> routingFields, List<FieldBinding> extraFields,
        String status, boolean removal, int version) {
}
