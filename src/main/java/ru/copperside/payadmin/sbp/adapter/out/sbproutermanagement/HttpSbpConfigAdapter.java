package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.sbp.application.config.ExtractionRuleRequest;
import ru.copperside.payadmin.sbp.application.config.RoutingFlagRequest;
import ru.copperside.payadmin.sbp.application.config.TerminalConfigRequest;
import ru.copperside.payadmin.sbp.application.config.TkbPayEntryRequest;
import ru.copperside.payadmin.sbp.application.config.UpstreamRequest;
import ru.copperside.payadmin.sbp.application.config.port.out.SbpConfigPort;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.ExtractionRule;
import ru.copperside.payadmin.sbp.domain.PendingChanges;
import ru.copperside.payadmin.sbp.domain.RoutingFlag;
import ru.copperside.payadmin.sbp.domain.RoutingManifest;
import ru.copperside.payadmin.sbp.domain.TerminalConfig;
import ru.copperside.payadmin.sbp.domain.TkbPayEntry;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class HttpSbpConfigAdapter implements SbpConfigPort {

    private static final String BASE = "/internal/v1/sbp-router-management";

    private static final ParameterizedTypeReference<SbpApiResponse<List<Upstream>>> UPSTREAMS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<Upstream>> UPSTREAM =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<List<ExtractionRule>>> RULES =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<ExtractionRule>> RULE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<TerminalConfig>> TERMINAL =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<List<TkbPayEntry>>> TKB_LIST =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<TkbPayEntry>> TKB =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<List<RoutingFlag>>> FLAGS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<RoutingFlag>> FLAG =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<PendingChanges>> PENDING =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<RoutingManifest>> MANIFEST =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<SbpApiResponse<Object>> VOID_RESPONSE =
            new ParameterizedTypeReference<>() { };

    private final SbpRouterManagementProperties properties;
    private final RestClient restClient;

    public HttpSbpConfigAdapter(SbpRouterManagementProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public List<Upstream> listUpstreams() {
        return call("sbp upstream list failed", () -> dataList(restClient.get()
                .uri(BASE + "/upstreams").headers(this::addHeaders).retrieve().body(UPSTREAMS)));
    }

    @Override
    public Upstream createUpstream(UpstreamRequest request) {
        return call("sbp upstream create failed", () -> data(restClient.post()
                .uri(BASE + "/upstreams").contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(UPSTREAM)));
    }

    @Override
    public Upstream patchUpstream(UUID id, UpstreamRequest request) {
        return call("sbp upstream patch failed", () -> data(restClient.patch()
                .uri(BASE + "/upstreams/{id}", id).contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(UPSTREAM)));
    }

    @Override
    public Upstream removeUpstream(UUID id) {
        return call("sbp upstream remove failed", () -> data(restClient.delete()
                .uri(BASE + "/upstreams/{id}", id).headers(this::addHeaders).retrieve().body(UPSTREAM)));
    }

    @Override
    public List<ExtractionRule> listExtractionRules() {
        return call("sbp extraction-rule list failed", () -> dataList(restClient.get()
                .uri(BASE + "/extraction-rules").headers(this::addHeaders).retrieve().body(RULES)));
    }

    @Override
    public ExtractionRule createExtractionRule(ExtractionRuleRequest request) {
        return call("sbp extraction-rule create failed", () -> data(restClient.post()
                .uri(BASE + "/extraction-rules").contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(RULE)));
    }

    @Override
    public ExtractionRule patchExtractionRule(UUID id, ExtractionRuleRequest request) {
        return call("sbp extraction-rule patch failed", () -> data(restClient.patch()
                .uri(BASE + "/extraction-rules/{id}", id).contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(RULE)));
    }

    @Override
    public ExtractionRule removeExtractionRule(UUID id) {
        return call("sbp extraction-rule remove failed", () -> data(restClient.delete()
                .uri(BASE + "/extraction-rules/{id}", id).headers(this::addHeaders).retrieve().body(RULE)));
    }

    @Override
    public TerminalConfig getTerminalConfig() {
        return call("sbp terminal-config get failed", () -> data(restClient.get()
                .uri(BASE + "/terminal-config").headers(this::addHeaders).retrieve().body(TERMINAL)));
    }

    @Override
    public TerminalConfig putTerminalConfig(TerminalConfigRequest request) {
        return call("sbp terminal-config put failed", () -> data(restClient.put()
                .uri(BASE + "/terminal-config").contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(TERMINAL)));
    }

    @Override
    public List<TkbPayEntry> listTkbPay() {
        return call("sbp tkb-pay list failed", () -> dataList(restClient.get()
                .uri(BASE + "/tkb-pay-list").headers(this::addHeaders).retrieve().body(TKB_LIST)));
    }

    @Override
    public TkbPayEntry addTkbPay(TkbPayEntryRequest request) {
        return call("sbp tkb-pay add failed", () -> data(restClient.post()
                .uri(BASE + "/tkb-pay-list").contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(TKB)));
    }

    @Override
    public TkbPayEntry removeTkbPay(UUID id) {
        return call("sbp tkb-pay remove failed", () -> data(restClient.delete()
                .uri(BASE + "/tkb-pay-list/{id}", id).headers(this::addHeaders).retrieve().body(TKB)));
    }

    @Override
    public List<RoutingFlag> listRoutingFlags() {
        return call("sbp routing-flags list failed", () -> dataList(restClient.get()
                .uri(BASE + "/routing-flags").headers(this::addHeaders).retrieve().body(FLAGS)));
    }

    @Override
    public RoutingFlag setRoutingFlag(String key, RoutingFlagRequest request) {
        return call("sbp routing-flag set failed", () -> data(restClient.put()
                .uri(BASE + "/routing-flags/{key}", key).contentType(MediaType.APPLICATION_JSON).body(request)
                .headers(this::addHeaders).retrieve().body(FLAG)));
    }

    @Override
    public PendingChanges pendingChanges() {
        return call("sbp pending-changes failed", () -> data(restClient.get()
                .uri(BASE + "/pending-changes").headers(this::addHeaders).retrieve().body(PENDING)));
    }

    @Override
    public void discardDrafts() {
        call("sbp discard drafts failed", () -> {
            restClient.delete().uri(BASE + "/drafts").headers(this::addHeaders).retrieve().body(VOID_RESPONSE);
            return null;
        });
    }

    @Override
    public RoutingManifest publishManifest() {
        return call("sbp manifest publish failed", () -> data(restClient.post()
                .uri(BASE + "/routing-manifests").headers(this::addHeaders).retrieve().body(MANIFEST)));
    }

    @Override
    public RoutingManifest latestManifest() {
        return call("sbp latest manifest failed", () -> data(restClient.get()
                .uri(BASE + "/routing-manifests/latest").headers(this::addHeaders).retrieve().body(MANIFEST)));
    }

    @Override
    public RoutingManifest getManifest(UUID manifestId) {
        return call("sbp manifest get failed", () -> data(restClient.get()
                .uri(BASE + "/routing-manifests/{id}", manifestId).headers(this::addHeaders).retrieve().body(MANIFEST)));
    }

    private static <T> T data(SbpApiResponse<T> response) {
        return response == null ? null : response.data();
    }

    private static <T> List<T> dataList(SbpApiResponse<List<T>> response) {
        return response == null || response.data() == null ? List.of() : response.data();
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
