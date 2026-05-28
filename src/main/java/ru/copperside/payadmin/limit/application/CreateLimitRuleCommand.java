package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.LimitRuleSelector;
import ru.copperside.payadmin.limit.domain.LimitTargetType;
import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;
import ru.copperside.payadmin.limit.domain.OperationDirection;

public record CreateLimitRuleCommand(
        String code,
        String name,
        LimitRuleSelector operationSelector,
        OperationDirection direction,
        LimitRuleSelector attributeSelector,
        LimitTargetType targetType,
        LimitRuleMetric metric,
        LimitRulePeriod period,
        String currency
) {
}
