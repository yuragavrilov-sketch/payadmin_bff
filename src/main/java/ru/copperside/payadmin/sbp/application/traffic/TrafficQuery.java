package ru.copperside.payadmin.sbp.application.traffic;

import java.time.Instant;

public record TrafficQuery(
        String requestType, String terminalOwner, String upstream, String outcome, String status,
        Instant from, Instant to, String q, String operationId, Integer page, Integer size) {
}
