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
import ru.copperside.payadmin.sbp.application.fleet.port.out.SbpFleetPort;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.RouterFleet;

@Component
public class HttpSbpFleetAdapter implements SbpFleetPort {

    private static final String ROUTERS = "/internal/v1/sbp-router-management/routers";

    private static final ParameterizedTypeReference<SbpApiResponse<RouterFleet>> FLEET =
            new ParameterizedTypeReference<>() { };

    private final SbpRouterManagementProperties properties;
    private final RestClient restClient;

    public HttpSbpFleetAdapter(SbpRouterManagementProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public RouterFleet listRouters() {
        try {
            SbpApiResponse<RouterFleet> response = restClient.get()
                    .uri(ROUTERS)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(FLEET);
            return response == null ? null : response.data();
        } catch (RestClientResponseException ex) {
            ProblemEnvelope problem = ex.getResponseBodyAs(ProblemEnvelope.class);
            if (problem != null && problem.error() != null) {
                throw new UpstreamProblemException(ex.getStatusCode(), problem);
            }
            throw new UpstreamUnavailableException("sbp routers list failed", ex);
        } catch (ResourceAccessException ex) {
            throw new UpstreamUnavailableException("sbp routers list failed", ex);
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
