package ru.copperside.payadmin.sbp.domain;

import java.util.UUID;

public record RoutingFlag(UUID id, String key, String value, String status) {
}
