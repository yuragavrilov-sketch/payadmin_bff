package ru.copperside.payadmin.limit.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RuntimeManifest(
        UUID id,
        int version,
        String status,
        String checksum,
        Instant createdAt,
        Instant effectiveFrom,
        int ruleCount,
        int assignmentCount,
        int membershipCount,
        List<JsonNode> rules,
        List<JsonNode> assignments,
        List<JsonNode> memberships,
        List<JsonNode> diagnostics
) {
    public RuntimeManifest {
        rules = rules == null ? List.of() : List.copyOf(rules);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
        memberships = memberships == null ? List.of() : List.copyOf(memberships);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public record Descriptor(
            UUID id,
            int version,
            String checksum,
            Instant createdAt,
            Instant effectiveFrom,
            String lifecycleStatus
    ) {
    }
}
