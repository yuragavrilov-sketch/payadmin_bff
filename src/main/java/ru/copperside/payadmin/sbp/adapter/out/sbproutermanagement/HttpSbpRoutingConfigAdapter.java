package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.sbp.application.routingconfig.port.out.SbpRoutingConfigPort;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.RoutingConfig;

import java.util.function.Supplier;

@Component
public class HttpSbpRoutingConfigAdapter implements SbpRoutingConfigPort {

    private static final String PATH = "/internal/v1/sbp-router-management/routing-config";

    private static final ParameterizedTypeReference<SbpApiResponse<RoutingConfig>> CONFIG =
            new ParameterizedTypeReference<>() { };

    private final SbpRouterManagementProperties properties;
    private final RestClient restClient;

    public HttpSbpRoutingConfigAdapter(SbpRouterManagementProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public RoutingConfig get() {
        return call("sbp routing-config get failed", () -> {
            SbpApiResponse<RoutingConfig> response = restClient.get()
                    .uri(PATH)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(CONFIG);
            return response == null ? null : response.data();
        });
    }

    @Override
    public RoutingConfig replace(RoutingConfig config) {
        return call("sbp routing-config put failed", () -> {
            SbpApiResponse<RoutingConfig> response = restClient.put()
                    .uri(PATH)
                    .headers(this::addHeaders)
                    .body(config)
                    .retrieve()
                    .body(CONFIG);
            return response == null ? null : response.data();
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
