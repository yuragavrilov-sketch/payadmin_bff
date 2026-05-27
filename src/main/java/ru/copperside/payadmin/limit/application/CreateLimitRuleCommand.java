package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLimitRuleCommand(
        String code,
        String name,
        UUID operationTypeId,
        LimitRuleMetric metric,
        LimitRulePeriod period,
        BigDecimal amountLimit,
        Long countLimit
) {
    public CreateLimitRuleCommand(
            String code,
            String name,
            UUID operationTypeId,
            LimitRuleMetric metric,
            LimitRulePeriod period
    ) {
        this(code, name, operationTypeId, metric, period, null, null);
    }
}
