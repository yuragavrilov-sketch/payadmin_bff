package ru.copperside.payadmin.terminal.adapter.out.merchantscore;

import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.merchant.adapter.out.merchantscore.MerchantsCoreApiResponse;
import ru.copperside.payadmin.merchant.config.MerchantsCoreProperties;
import ru.copperside.payadmin.terminal.application.port.out.TerminalCatalogPort;
import ru.copperside.payadmin.terminal.application.port.out.TerminalLine;
import ru.copperside.payadmin.terminal.application.port.out.TerminalPage;
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

@Component
public class HttpTerminalCatalogAdapter implements TerminalCatalogPort {

    private static final ParameterizedTypeReference<MerchantsCoreApiResponse<List<MerchantsCoreTerminalLine>>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final MerchantsCoreProperties properties;
    private final RestClient restClient;

    public HttpTerminalCatalogAdapter(MerchantsCoreProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public TerminalPage fetchPage(int limit, int offset, String search, String sortBy, String sortDir) {
        try {
            MerchantsCoreApiResponse<List<MerchantsCoreTerminalLine>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/api/v1/terminals")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (search != null && !search.isBlank()) {
                            uri.queryParam("search", search);
                        }
                        return uri.queryParam("sortBy", sortBy).queryParam("sortDir", sortDir).build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(PAGE_TYPE);
            return toPage(response);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("merchants-core terminals request failed", ex);
        }
    }

    @Override
    public TerminalPage fetchByMerchant(long mercId, int limit, int offset, String search, String sortBy, String sortDir) {
        try {
            MerchantsCoreApiResponse<List<MerchantsCoreTerminalLine>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/api/v1/merchants/{mercId}/terminals")
                                .queryParam("limit", limit)
                                .queryParam("offset", offset);
                        if (search != null && !search.isBlank()) {
                            uri.queryParam("search", search);
                        }
                        return uri.queryParam("sortBy", sortBy).queryParam("sortDir", sortDir).build(mercId);
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(PAGE_TYPE);
            return toPage(response);
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException("merchants-core merchant terminals request failed", ex);
        }
    }

    private TerminalPage toPage(MerchantsCoreApiResponse<List<MerchantsCoreTerminalLine>> response) {
        if (response == null || response.data() == null) {
            return new TerminalPage(List.of(), 0L);
        }
        List<TerminalLine> lines = response.data().stream()
                .map(dto -> new TerminalLine(
                        dto.mercId(), dto.mps(), dto.gate(), dto.is3ds(), dto.terminalId(), dto.merchantId(),
                        dto.mcc(), dto.name(), dto.merchantUrl(), dto.login(), dto.hasPassword(),
                        dto.apiUrl(), dto.merchantName()))
                .toList();
        long total = response.meta() == null || response.meta().total() == null
                ? lines.size()
                : response.meta().total();
        return new TerminalPage(lines, total);
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
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
