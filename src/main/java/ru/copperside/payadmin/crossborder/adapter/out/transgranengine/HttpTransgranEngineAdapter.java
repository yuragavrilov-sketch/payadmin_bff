package ru.copperside.payadmin.crossborder.adapter.out.transgranengine;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.crossborder.application.port.out.CrossBorderEnginePort;
import ru.copperside.payadmin.crossborder.config.TransgranEngineProperties;
import ru.copperside.payadmin.crossborder.domain.CrossBorderOperation;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.util.List;
import java.util.Map;

@Component
public class HttpTransgranEngineAdapter implements CrossBorderEnginePort {

    private static final ParameterizedTypeReference<TransgranApiResponse<List<PartnerCountry>>> BANKS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<TransgranApiResponse<List<CrossBorderOperation>>> OPS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<TransgranApiResponse<TransferSettings>> SETTINGS_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /** op → payout-путь engine (тестовый passthrough). */
    private static final Map<String, String> PAYOUT_PATHS = Map.of(
            "convert", "/v1/payout/currency/convert",
            "create", "/v1/payout/create",
            "get", "/v1/payout/get");

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
        try {
            TransgranApiResponse<List<CrossBorderOperation>> response = restClient.get()
                    .uri(builder -> builder.path("/internal/admin/transgran/operations")
                            .queryParam("limit", limit).queryParam("offset", offset).build())
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(OPS_TYPE);
            if (response == null || response.data() == null) {
                return new OperationsPage(List.of(), 0L);
            }
            long total = response.meta() == null || response.meta().total() == null
                    ? response.data().size()
                    : response.meta().total();
            return new OperationsPage(response.data(), total);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("transgran-engine operations request failed", ex);
        }
    }

    @Override
    public TransferSettings getSettings() {
        try {
            TransgranApiResponse<TransferSettings> response = restClient.get()
                    .uri("/internal/admin/transgran/settings")
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(SETTINGS_TYPE);
            if (response == null || response.data() == null) {
                throw new UpstreamUnavailableException("transgran-engine settings request returned no data", null);
            }
            return response.data();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("transgran-engine settings request failed", ex);
        }
    }

    @Override
    public TransferSettings updateSettings(TransferSettingsUpdate update) {
        try {
            TransgranApiResponse<TransferSettings> response = restClient.put()
                    .uri("/internal/admin/transgran/settings")
                    .headers(this::addHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(update)
                    .retrieve()
                    .body(SETTINGS_TYPE);
            if (response == null || response.data() == null) {
                throw new UpstreamUnavailableException("transgran-engine settings update returned no data", null);
            }
            return response.data();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("transgran-engine settings update failed", ex);
        }
    }

    @Override
    public JsonNode proxyPayout(String op, JsonNode body) {
        String path = PAYOUT_PATHS.get(op);
        if (path == null) {
            throw new IllegalArgumentException("unknown payout op: " + op);
        }
        try {
            // Тест-страница: пробрасываем фактический статус + тело апстрима (включая 4xx wirebank,
            // напр. валидацию/токен), а не маскируем под 503. Только 5xx/сеть → UpstreamUnavailable.
            ResponseEntity<JsonNode> resp = restClient.post()
                    .uri(path)
                    .headers(this::addHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> { })
                    .toEntity(JsonNode.class);
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            out.put("status", resp.getStatusCode().value());
            out.set("body", resp.getBody() != null ? resp.getBody() : JsonNodeFactory.instance.nullNode());
            return out;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("transgran-engine payout " + op + " failed", ex);
        }
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
