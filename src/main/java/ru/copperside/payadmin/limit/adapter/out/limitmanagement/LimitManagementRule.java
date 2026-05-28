package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;
import ru.copperside.payadmin.limit.domain.LimitRuleSelector;
import ru.copperside.payadmin.limit.domain.LimitRuleStatus;
import ru.copperside.payadmin.limit.domain.LimitTargetType;
import ru.copperside.payadmin.limit.domain.OperationDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record LimitManagementRule(
        UUID id,
        String code,
        int version,
        String name,
        String direction,
        LimitManagementSelector operationSelector,
        LimitManagementSelector attributeSelector,
        String targetType,
        String metric,
        String period,
        String currency,
        BigDecimal amountLimit,
        Long countLimit,
        String status,
        Boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant disabledAt
) {
    LimitRule toDomain() {
        LimitRuleStatus resolvedStatus = enumValue(LimitRuleStatus.class, status);
        boolean resolvedEnabled = resolvedStatus == LimitRuleStatus.ACTIVE;
        return new LimitRule(
                id,
                code,
                version,
                name,
                enumValue(OperationDirection.class, direction),
                selector(operationSelector),
                selector(attributeSelector),
                enumValue(LimitTargetType.class, targetType),
                enumValue(LimitRuleMetric.class, metric),
                enumValue(LimitRulePeriod.class, period),
                currency,
                amountLimit,
                countLimit,
                resolvedStatus,
                createdAt,
                updatedAt,
                activatedAt,
                disabledAt,
                enabled == null ? resolvedEnabled : enabled
        );
    }

    private static LimitRuleSelector selector(LimitManagementSelector selector) {
        return selector == null ? null : new LimitRuleSelector(selector.type(), selector.value());
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    record LimitManagementSelector(String type, String value) {
    }
}
