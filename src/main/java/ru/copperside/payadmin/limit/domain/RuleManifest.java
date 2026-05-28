package ru.copperside.payadmin.limit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuleManifest(
        UUID id,
        int version,
        String status,
        String checksum,
        int ruleCount,
        Instant createdAt,
        List<CompiledRule> rules,
        List<Diagnostic> diagnostics
) {
    public RuleManifest {
        rules = rules == null ? List.of() : List.copyOf(rules);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public record CompiledRule(
            UUID ruleId,
            String code,
            int version,
            Matcher matcher,
            Measure measure
    ) {
    }

    public record Matcher(
            LimitRuleSelector operation,
            OperationDirection direction,
            LimitRuleSelector attribute,
            LimitTargetType targetType
    ) {
    }

    public record Measure(
            LimitRuleMetric metric,
            LimitRulePeriod period,
            String currency
    ) {
    }

    public record Diagnostic(
            String code,
            String severity,
            String message,
            List<UUID> ruleIds,
            String path
    ) {
        public Diagnostic {
            ruleIds = ruleIds == null ? List.of() : List.copyOf(ruleIds);
        }
    }
}
