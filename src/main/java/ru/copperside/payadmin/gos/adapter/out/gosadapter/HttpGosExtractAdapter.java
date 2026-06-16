package ru.copperside.payadmin.gos.adapter.out.gosadapter;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.gos.adapter.out.gosadapter.GosAdapterRequests.ExtractBody;
import ru.copperside.payadmin.gos.adapter.out.gosadapter.GosAdapterRequests.ExtractStatusBody;
import ru.copperside.payadmin.gos.application.GosExtractCommand;
import ru.copperside.payadmin.gos.application.GosExtractResult;
import ru.copperside.payadmin.gos.application.GosExtractStatusQuery;
import ru.copperside.payadmin.gos.application.GosExtractStatusResult;
import ru.copperside.payadmin.gos.application.port.out.GosExtractPort;

import java.util.function.Supplier;

@Component
public class HttpGosExtractAdapter implements GosExtractPort {

    static final String MERCH_ID_HEADER = "TCB.Header-Merch-Id";

    private static final String BASE = "/api/v1/nominal/egrul-egrip";

    private static final ParameterizedTypeReference<GosExtractResult> EXTRACT =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<GosExtractStatusResult> STATUS =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;

    public HttpGosExtractAdapter(@Qualifier("gosRestClient") RestClient gosRestClient) {
        this.restClient = gosRestClient;
    }

    @Override
    public GosExtractResult requestExtract(GosExtractCommand command) {
        ExtractBody body = new ExtractBody(command.beneficiaryType(), command.inn(), command.name());
        return call("gos extract request failed", () -> restClient.post()
                .uri(BASE + "/getExtract")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> addHeaders(headers, command.merchId()))
                .body(body)
                .retrieve()
                .body(EXTRACT));
    }

    @Override
    public GosExtractStatusResult getStatus(GosExtractStatusQuery query) {
        ExtractStatusBody body = new ExtractStatusBody(query.extractRequestId());
        return call("gos extract status failed", () -> restClient.post()
                .uri(BASE + "/getExtractStatus")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> addHeaders(headers, query.merchId()))
                .body(body)
                .retrieve()
                .body(STATUS));
    }

    private <T> T call(String message, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ResourceAccessException ex) {
            throw new UpstreamUnavailableException(message, ex);
        }
    }

    private void addHeaders(HttpHeaders headers, String merchId) {
        if (merchId != null && !merchId.isBlank()) {
            headers.set(MERCH_ID_HEADER, merchId);
        }
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            headers.set(RequestIdFilter.HEADER_NAME, traceId);
        }
    }
}
