package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.sbp.application.traffic.TrafficQuery;
import ru.copperside.payadmin.sbp.application.traffic.port.out.SbpTrafficPort;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;
import ru.copperside.payadmin.sbp.domain.TrafficTransaction;

import java.time.Instant;
import java.util.function.Supplier;

@Component
public class HttpSbpTrafficAdapter implements SbpTrafficPort {

    private static final String BASE = "/internal/v1/sbp-router-management/traffic";

    private static final ParameterizedTypeReference<SbpApiResponse<TrafficListResult>> LIST =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<TrafficTransaction>> ONE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<StatsData>> STATS =
            new ParameterizedTypeReference<>() { };

    private record StatsData(TrafficStats stats) {
    }

    private final SbpRouterManagementProperties properties;
    private final RestClient restClient;

    public HttpSbpTrafficAdapter(SbpRouterManagementProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public TrafficListResult listTransactions(TrafficQuery query) {
        return call("sbp traffic list failed", () -> {
            SbpApiResponse<TrafficListResult> response = restClient.get()
                    .uri(builder -> buildListUri(builder, query))
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(LIST);
            return response == null ? null : response.data();
        });
    }

    @Override
    public TrafficTransaction getTransaction(String correlationId) {
        return call("sbp traffic detail failed", () -> {
            SbpApiResponse<TrafficTransaction> response = restClient.get()
                    .uri(BASE + "/transactions/{correlationId}", correlationId)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(ONE);
            return response == null ? null : response.data();
        });
    }

    @Override
    public TrafficStats stats(Instant from, Instant to) {
        return call("sbp traffic stats failed", () -> {
            SbpApiResponse<StatsData> response = restClient.get()
                    .uri(builder -> {
                        builder.path(BASE + "/stats");
                        if (from != null) {
                            builder.queryParam("from", from.toString());
                        }
                        if (to != null) {
                            builder.queryParam("to", to.toString());
                        }
                        return builder.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(STATS);
            return response == null || response.data() == null ? null : response.data().stats();
        });
    }

    private java.net.URI buildListUri(UriBuilder builder, TrafficQuery q) {
        builder.path(BASE + "/transactions");
        addParam(builder, "requestType", q.requestType());
        addParam(builder, "terminalOwner", q.terminalOwner());
        addParam(builder, "upstream", q.upstream());
        addParam(builder, "outcome", q.outcome());
        addParam(builder, "status", q.status());
        addParam(builder, "q", q.q());
        addParam(builder, "operationId", q.operationId());
        if (q.from() != null) {
            builder.queryParam("from", q.from().toString());
        }
        if (q.to() != null) {
            builder.queryParam("to", q.to().toString());
        }
        builder.queryParam("page", q.page() == null ? 0 : q.page());
        builder.queryParam("size", q.size() == null ? 50 : q.size());
        return builder.build();
    }

    private static void addParam(UriBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, value);
        }
    }

    private <T> T call(String message, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException ex) {
            ProblemEnvelope problem = ex.getResponseBodyAs(ProblemEnvelope.class);
            if (problem != null && problem.error() != null) {
                throw new UpstreamProblemException(ex.getStatusCode(), problem);
            }
            throw new UpstreamUnavailableException(message, ex);
        } catch (ResourceAccessException ex) {
            throw new UpstreamUnavailableException(message, ex);
        }
    }

    private void addHeaders(HttpHeaders headers) {
        if (!properties.internalAdminApiKey().isBlank()) {
            headers.set(properties.internalAdminHeaderName(), properties.internalAdminApiKey());
        }
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            headers.set(RequestIdFilter.HEADER_NAME, traceId);
        }
    }
}
