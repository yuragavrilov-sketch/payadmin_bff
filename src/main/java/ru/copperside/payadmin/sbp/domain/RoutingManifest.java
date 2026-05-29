package ru.copperside.payadmin.sbp.domain;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record RoutingManifest(
        UUID id, Integer version, String status, String checksum, Instant createdAt,
        JsonNode payload, JsonNode diagnostics) {
}
