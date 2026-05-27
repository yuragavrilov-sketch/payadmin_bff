package ru.copperside.payadmin.limit.adapter.in.web;

import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.LimitRuleSelector;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LimitRuleResponse(
        UUID id,
        String code,
        int version,
        String name,
        UUID operationTypeId,
        String operationTypeCode,
        String operationTypeDirection,
        String direction,
        Selector operationSelector,
        Selector attributeSelector,
        String targetType,
        String metric,
        String period,
        String currency,
        BigDecimal amountLimit,
        Long countLimit,
        String status,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        Instant disabledAt
) {
    static LimitRuleResponse from(LimitRule rule) {
        String operationTypeCode = rule.operationTypeCode();
        Selector operationSelector = selector(rule.operationSelector());
        if (operationSelector == null && operationTypeCode != null) {
            operationSelector = new Selector("TYPE", operationTypeCode);
        }
        Selector attributeSelector = selector(rule.attributeSelector());
        if (attributeSelector == null) {
            attributeSelector = new Selector("NONE", null);
        }
        String direction = rule.operationTypeDirection() == null ? null : rule.operationTypeDirection().name();
        return new LimitRuleResponse(
                rule.id(),
                rule.code(),
                rule.version(),
                rule.name(),
                rule.operationTypeId(),
                operationTypeCode,
                direction,
                direction,
                operationSelector,
                attributeSelector,
                rule.targetType() == null ? null : rule.targetType().name(),
                rule.metric() == null ? null : rule.metric().name(),
                rule.period() == null ? null : rule.period().name(),
                rule.currency(),
                rule.amountLimit(),
                rule.countLimit(),
                rule.status() == null ? null : rule.status().name(),
                rule.enabled(),
                rule.createdAt(),
                rule.updatedAt(),
                rule.activatedAt(),
                rule.disabledAt()
        );
    }

    private static Selector selector(LimitRuleSelector selector) {
        if (selector == null) {
            return null;
        }
        return new Selector(selector.type(), selector.value());
    }

    public record Selector(String type, String value) {
    }
}
