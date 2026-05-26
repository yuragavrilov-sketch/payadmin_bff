package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationEntry;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.merchant.config.MerchantsCoreProperties;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class HttpMerchantsCoreAdapter implements MerchantCatalogPort {

    private static final ParameterizedTypeReference<MerchantsCoreApiResponse<List<MerchantsCoreMerchantWithConfigLine>>> ACTIVE_LINES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<MerchantsCoreApiResponse<List<MerchantsCoreConfigEntry>>> CONFIGURATION_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<MerchantsCoreApiResponse<MerchantsCoreCount>> COUNT_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final MerchantsCoreProperties properties;
    private final RestClient restClient;

    public HttpMerchantsCoreAdapter(MerchantsCoreProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir) {
        try {
            MerchantsCoreApiResponse<List<MerchantsCoreMerchantWithConfigLine>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/api/v1/merchants/configurations/active-line")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (search != null && !search.isBlank()) {
                            uri.queryParam("search", search);
                        }
                        return uri.queryParam("sortBy", sortBy)
                                .queryParam("sortDir", sortDir)
                                .build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(ACTIVE_LINES_TYPE);

            if (response == null || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                    .map(dto -> new MerchantConfigurationLine(
                            dto.mercId(),
                            dto.name(),
                            dto.configuration() == null ? Map.of() : dto.configuration(),
                            dto.activeSince()
                    ))
                    .toList();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("merchants-core active-line request failed", ex);
        }
    }

    @Override
    public List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId) {
        try {
            MerchantsCoreApiResponse<List<MerchantsCoreConfigEntry>> response = restClient.get()
                    .uri(builder -> builder.path("/api/v1/merchants/{merchantId}/configurations")
                            .build(merchantId))
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(CONFIGURATION_TYPE);

            if (response == null || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                    .map(dto -> new MerchantConfigurationEntry(
                            dto.parameterName(),
                            dto.parameterValue(),
                            dto.dateBegin(),
                            dto.dateEnd()
                    ))
                    .toList();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("merchants-core configuration request failed", ex);
        }
    }

    @Override
    public long countActiveLines(String search) {
        try {
            MerchantsCoreApiResponse<MerchantsCoreCount> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/api/v1/merchants/configurations/active-line/count");
                        if (search != null && !search.isBlank()) {
                            uri.queryParam("search", search);
                        }
                        return uri.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(COUNT_TYPE);

            if (response == null || response.data() == null) {
                return 0L;
            }
            return response.data().total();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("merchants-core active-line count request failed", ex);
        }
    }

    private SimpleClientHttpRequestFactory requestFactory(MerchantsCoreProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
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

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}

