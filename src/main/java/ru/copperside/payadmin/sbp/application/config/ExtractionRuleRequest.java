package ru.copperside.payadmin.sbp.application.config;

import ru.copperside.payadmin.sbp.domain.FieldBinding;

import java.util.List;

public record ExtractionRuleRequest(
        String messageType, List<FieldBinding> routingFields, List<FieldBinding> extraFields) {
}
