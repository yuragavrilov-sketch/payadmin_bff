package ru.copperside.payadmin.sbp.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrafficTransaction(
        String correlationId, String txId, String requestType, String terminalOwner, String route,
        String upstream, String outcome, String status, Instant requestAt, Instant responseAt,
        Long latencyMs, String env, String requestXml, String responseXml,
        String operationId, String operationType) {
}
