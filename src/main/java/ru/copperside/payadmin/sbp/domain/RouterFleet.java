package ru.copperside.payadmin.sbp.domain;

import java.time.Instant;
import java.util.List;

/** The running sbp-router fleet as reported by sbp-router-management's /routers endpoint. */
public record RouterFleet(int total, int up, List<RouterInstance> routers) {

    public record RouterInstance(
            String instanceId,
            String status,
            Instant startedAt,
            Instant lastHeartbeat,
            String activeGroup,
            List<String> groups,
            List<RouterBackend> backends,
            RouterMetrics metrics
    ) {
    }

    public record RouterBackend(String url, String group, boolean banned) {
    }

    public record RouterMetrics(
            int activeRequests,
            double requestsTotal,
            double upstreamErrorsTotal,
            double kafkaPublishedTotal,
            long requestCount,
            double avgLatencyMs,
            double maxLatencyMs
    ) {
    }
}
