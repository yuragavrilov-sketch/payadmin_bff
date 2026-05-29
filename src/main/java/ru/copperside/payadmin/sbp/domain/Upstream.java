package ru.copperside.payadmin.sbp.domain;

import java.time.Instant;
import java.util.UUID;

public record Upstream(
        UUID id, String name, String url, Integer timeoutMs, Integer retryMaxAttempts,
        Integer retryBackoffMs, String status, boolean removal, int version, Instant updatedAt) {
}
