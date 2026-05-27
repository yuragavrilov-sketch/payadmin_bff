package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;

import java.util.UUID;

public record CreateLimitRuleCommand(
        String code,
        String name,
        UUID operationTypeId,
        LimitRuleMetric metric,
        LimitRulePeriod period
) {
}
