package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;

import java.math.BigDecimal;
import java.util.UUID;

public record PatchLimitRuleCommand(
        String name,
        UUID operationTypeId,
        LimitRuleMetric metric,
        LimitRulePeriod period,
        BigDecimal amountLimit,
        Long countLimit
) {
    public PatchLimitRuleCommand(
            String name,
            UUID operationTypeId,
            LimitRuleMetric metric,
            LimitRulePeriod period
    ) {
        this(name, operationTypeId, metric, period, null, null);
    }
}
