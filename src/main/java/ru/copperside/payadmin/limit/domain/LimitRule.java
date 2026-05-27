package ru.copperside.payadmin.limit.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LimitRule(
        UUID id,
        String code,
        int version,
        String name,
        UUID operationTypeId,
        String operationTypeCode,
        OperationDirection operationTypeDirection,
        LimitTargetType targetType,
        LimitRuleMetric metric,
        LimitRulePeriod period,
        String currency,
        BigDecimal amountLimit,
        Long countLimit,
        LimitRuleStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant disabledAt,
        boolean enabled,
        LimitRuleSelector operationSelector,
        LimitRuleSelector attributeSelector
) {
}
