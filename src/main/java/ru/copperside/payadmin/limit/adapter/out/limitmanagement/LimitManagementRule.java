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
        UUID operationTypeId,
        String operationTypeCode,
        String operationTypeDirection,
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
        String resolvedOperationTypeCode = operationTypeCode != null
                ? operationTypeCode
                : operationSelector == null ? null : operationSelector.value();
        OperationDirection resolvedDirection = enumValue(
                OperationDirection.class,
                operationTypeDirection != null ? operationTypeDirection : direction
        );
        LimitRuleStatus resolvedStatus = enumValue(LimitRuleStatus.class, status);
        boolean resolvedEnabled = resolvedStatus == LimitRuleStatus.ACTIVE;
        return new LimitRule(
                id,
                code,
                version,
                name,
                operationTypeId,
                resolvedOperationTypeCode,
                resolvedDirection,
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
                resolvedEnabled,
                selector(operationSelector, "TYPE", resolvedOperationTypeCode),
                selector(attributeSelector, "NONE", null)
        );
    }

    private static LimitRuleSelector selector(LimitManagementSelector selector, String defaultType, String defaultValue) {
        if (selector != null) {
            return new LimitRuleSelector(selector.type(), selector.value());
        }
        if (defaultType == null) {
            return null;
        }
        return new LimitRuleSelector(defaultType, defaultValue);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    record LimitManagementSelector(String type, String value) {
    }
}
