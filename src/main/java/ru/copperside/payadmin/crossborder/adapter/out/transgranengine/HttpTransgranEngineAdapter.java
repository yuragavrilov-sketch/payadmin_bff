package ru.copperside.payadmin.crossborder.adapter.out.transgranengine;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.crossborder.application.port.out.CrossBorderEnginePort;
import ru.copperside.payadmin.crossborder.config.TransgranEngineProperties;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.util.List;

@Component
public class HttpTransgranEngineAdapter implements CrossBorderEnginePort {

    private static final ParameterizedTypeReference<TransgranApiResponse<List<PartnerCountry>>> BANKS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final TransgranEngineProperties properties;
    private final RestClient restClient;

    public HttpTransgranEngineAdapter(TransgranEngineProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<PartnerCountry> listBanks() {
        try {
            TransgranApiResponse<List<PartnerCountry>> response = restClient.get()
                    .uri("/internal/admin/transgran/banks")
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(BANKS_TYPE);
            return response == null || response.data() == null ? List.of() : response.data();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("transgran-engine banks request failed", ex);
        }
    }

    @Override
    public OperationsPage listOperations(int limit, int offset) {
        throw new UnsupportedOperationException("implemented in Task 4");
    }

    @Override
    public TransferSettings getSettings() {
        throw new UnsupportedOperationException("implemented in Task 4");
    }

    @Override
    public TransferSettings updateSettings(TransferSettingsUpdate update) {
        throw new UnsupportedOperationException("implemented in Task 4");
    }

    private SimpleClientHttpRequestFactory requestFactory(TransgranEngineProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
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

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
