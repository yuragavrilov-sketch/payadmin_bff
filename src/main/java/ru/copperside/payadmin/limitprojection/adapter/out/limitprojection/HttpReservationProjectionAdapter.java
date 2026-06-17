package ru.copperside.payadmin.limitprojection.adapter.out.limitprojection;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.limitprojection.application.port.out.ReservationProjectionPort;
import ru.copperside.payadmin.limitprojection.config.LimitProjectionProperties;
import ru.copperside.payadmin.limitprojection.domain.ReservationEvent;
import ru.copperside.payadmin.limitprojection.domain.ReservationState;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;
import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class HttpReservationProjectionAdapter implements ReservationProjectionPort {

    private static final String BASE = "/internal/v1/limit-projection";

    private static final ParameterizedTypeReference<LimitProjectionApiResponse<List<ReservationState>>> STATES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitProjectionApiResponse<ReservationState>> STATE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitProjectionApiResponse<List<ReservationEvent>>> EVENTS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitProjectionApiResponse<List<ReservationSummaryRow>>> SUMMARY_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final LimitProjectionProperties properties;
    private final RestClient restClient;

    public HttpReservationProjectionAdapter(LimitProjectionProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ReservationStatePage list(String merchantId, String state, String from, String to, int page, int size) {
        return call("limit-projection reservation list request failed", () -> {
            int upstreamPage = Math.max(page, 1) - 1;
            LimitProjectionApiResponse<List<ReservationState>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path(BASE + "/reservations");
                        if (notBlank(merchantId)) uri.queryParam("merchantId", merchantId);
                        if (notBlank(state)) uri.queryParam("state", state);
                        if (notBlank(from)) uri.queryParam("from", from);
                        if (notBlank(to)) uri.queryParam("to", to);
                        uri.queryParam("page", upstreamPage);
                        uri.queryParam("size", size);
                        return uri.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(STATES_TYPE);
            List<ReservationState> items = response == null || response.data() == null ? List.of() : response.data();
            return new ReservationStatePage(items, page, size);
        });
    }

    @Override
    public Optional<ReservationState> findByReservationId(String reservationId) {
        return call("limit-projection reservation get request failed", () -> {
            LimitProjectionApiResponse<ReservationState> response = restClient.get()
                    .uri(BASE + "/reservations/{id}", reservationId)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(STATE_TYPE);
            return Optional.ofNullable(response == null ? null : response.data());
        });
    }

    @Override
    public Optional<ReservationState> findByOperationId(String operationId) {
        return call("limit-projection reservation by-operation request failed", () -> {
            LimitProjectionApiResponse<ReservationState> response = restClient.get()
                    .uri(BASE + "/operations/{operationId}/reservation", operationId)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(STATE_TYPE);
            return Optional.ofNullable(response == null ? null : response.data());
        });
    }

    @Override
    public List<ReservationEvent> events(String reservationId) {
        return call("limit-projection reservation events request failed", () -> {
            LimitProjectionApiResponse<List<ReservationEvent>> response = restClient.get()
                    .uri(BASE + "/reservations/{id}/events", reservationId)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(EVENTS_TYPE);
            return response == null || response.data() == null ? List.of() : response.data();
        });
    }

    @Override
    public List<ReservationSummaryRow> summary(String merchantId, String from, String to, String groupBy) {
        return call("limit-projection reservation summary request failed", () -> {
            LimitProjectionApiResponse<List<ReservationSummaryRow>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path(BASE + "/reservations/summary");
                        if (notBlank(merchantId)) uri.queryParam("merchantId", merchantId);
                        if (notBlank(from)) uri.queryParam("from", from);
                        if (notBlank(to)) uri.queryParam("to", to);
                        if (notBlank(groupBy)) uri.queryParam("groupBy", groupBy);
                        return uri.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(SUMMARY_TYPE);
            return response == null || response.data() == null ? List.of() : response.data();
        });
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

    private JdkClientHttpRequestFactory requestFactory(LimitProjectionProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
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

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
