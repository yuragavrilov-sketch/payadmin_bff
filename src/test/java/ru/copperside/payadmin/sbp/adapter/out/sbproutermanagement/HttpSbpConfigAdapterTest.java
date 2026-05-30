package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.sbp.application.config.ExtractionRuleRequest;
import ru.copperside.payadmin.sbp.application.config.UpstreamRequest;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.RoutingManifest;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpSbpConfigAdapterTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private HttpSbpConfigAdapter adapter;

    private final SbpRouterManagementProperties properties = new SbpRouterManagementProperties(
            "http://sbp-mgmt:8087", "X-Internal-Admin-Key", "secret-key",
            Duration.ofSeconds(2), Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://sbp-mgmt:8087");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpSbpConfigAdapter(properties, builder.build());
    }

    @Test
    void listUpstreamsUnwrapsEnvelopeAndSendsAdminKey() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/upstreams"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Admin-Key", "secret-key"))
                .andRespond(withSuccess("""
                        {"data":[{"id":"11111111-1111-1111-1111-111111111111","name":"infosrv","url":"http://u",
                        "timeoutMs":30000,"retryMaxAttempts":2,"retryBackoffMs":500,"status":"ACTIVE","removal":false,
                        "version":1,"updatedAt":"2026-05-29T09:00:00Z"}],"meta":{},"error":null,
                        "timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        List<Upstream> result = adapter.listUpstreams();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("infosrv");
        server.verify();
    }

    @Test
    void createUpstreamPostsBodyAndReturnsData() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/upstreams"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"name":"infosrv","url":"http://u"}"""))
                .andRespond(withSuccess("""
                        {"data":{"id":"11111111-1111-1111-1111-111111111111","name":"infosrv","url":"http://u",
                        "timeoutMs":null,"retryMaxAttempts":null,"retryBackoffMs":null,"status":"DRAFT","removal":false,
                        "version":1,"updatedAt":"2026-05-29T09:00:00Z"},"meta":{},"error":null,
                        "timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        Upstream created = adapter.createUpstream(new UpstreamRequest("infosrv", "http://u", null, null, null));

        assertThat(created.status()).isEqualTo("DRAFT");
        server.verify();
    }

    @Test
    void patchExtractionRuleForwardsBodyAndReturnsData() {
        UUID ruleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/extraction-rules/" + ruleId))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("""
                        {"messageType":"ReqAuthPay","routingFields":[],"extraFields":[]}"""))
                .andRespond(withSuccess("""
                        {"data":{"id":"22222222-2222-2222-2222-222222222222","messageType":"ReqAuthPay",
                        "routingFields":[],"extraFields":[],"status":"DRAFT","removal":false,"version":2},
                        "meta":{},"error":null,"timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        var rule = adapter.patchExtractionRule(ruleId,
                new ExtractionRuleRequest("ReqAuthPay", List.of(), List.of()));

        assertThat(rule.messageType()).isEqualTo("ReqAuthPay");
        server.verify();
    }

    @Test
    void removeUpstreamSendsDeleteAndReturnsData() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/upstreams/11111111-1111-1111-1111-111111111111"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-Internal-Admin-Key", "secret-key"))
                .andRespond(withSuccess("""
                        {"data":{"id":"11111111-1111-1111-1111-111111111111","name":"infosrv","url":"http://u",
                        "timeoutMs":null,"retryMaxAttempts":null,"retryBackoffMs":null,"status":"DRAFT","removal":true,
                        "version":1,"updatedAt":"2026-05-29T09:00:00Z"},"meta":{},"error":null,
                        "timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        Upstream removed = adapter.removeUpstream(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));

        assertThat(removed.removal()).isTrue();
        server.verify();
    }

    @Test
    void upstreamProblemIsMappedToUpstreamProblemException() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/routing-manifests"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"error":{"type":"https://contracts.newpay/errors/routing-manifest-conflict",
                                "title":"Routing manifest conflict","status":409,"code":"ROUTING_MANIFEST_CONFLICT",
                                "message":"x","details":null,"traceId":"t"},"timestamp":"2026-05-29T09:00:00Z"}"""));

        assertThatThrownBy(() -> adapter.publishManifest())
                .isInstanceOf(UpstreamProblemException.class)
                .satisfies(ex -> assertThat(((UpstreamProblemException) ex).statusCode().value()).isEqualTo(409));
        server.verify();
    }

    // --- Regression: Jackson 2/3 manifest payload deserialization ---
    // Before fix, RoutingManifest.payload was typed as com.fasterxml.jackson.databind.JsonNode
    // (Jackson 2). The RestClient uses Jackson 3 (tools.jackson.databind), so deserializing a
    // manifest with a non-empty payload caused InvalidDefinitionException. This test reproduces
    // the exact path: MockRestServiceServer -> RestClient (Jackson 3) -> RoutingManifest.
    // It would fail with InvalidDefinitionException if the import were reverted to Jackson 2.

    @Test
    void latestManifestDeserializesNonEmptyPayloadAndDiagnostics() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/routing-manifests/latest"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"data":{
                          "id":"33333333-3333-3333-3333-333333333333",
                          "version":5,
                          "status":"ACTIVE",
                          "checksum":"abc123",
                          "createdAt":"2026-05-29T09:00:00Z",
                          "payload":{"upstreams":{"infosrv":{"url":"http://infosrv.bank.local/GCSvc"}}},
                          "diagnostics":[{"level":"INFO","message":"compiled ok"}]
                        },"meta":{},"error":null,"timestamp":"2026-05-29T09:00:00Z"}""",
                        MediaType.APPLICATION_JSON));

        RoutingManifest manifest = adapter.latestManifest();

        assertThat(manifest).isNotNull();
        assertThat(manifest.status()).isEqualTo("ACTIVE");
        // Assert payload was actually deserialized as a structured JsonNode, not null/failed
        assertThat(manifest.payload()).isNotNull();
        assertThat(manifest.payload().get("upstreams")).isNotNull();
        assertThat(manifest.payload().get("upstreams").get("infosrv").get("url").asText())
                .isEqualTo("http://infosrv.bank.local/GCSvc");
        // Assert diagnostics array deserialized correctly
        assertThat(manifest.diagnostics()).isNotNull();
        assertThat(manifest.diagnostics().isArray()).isTrue();
        assertThat(manifest.diagnostics().get(0).get("message").asText()).isEqualTo("compiled ok");
        server.verify();
    }

    @Test
    void publishManifestDeserializesNonEmptyPayload() {
        server.expect(requestTo("http://sbp-mgmt:8087/internal/v1/sbp-router-management/routing-manifests"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"data":{
                          "id":"44444444-4444-4444-4444-444444444444",
                          "version":6,
                          "status":"ACTIVE",
                          "checksum":"def456",
                          "createdAt":"2026-05-29T10:00:00Z",
                          "payload":{"upstreams":{"stub":{"url":"http://stub.local"}}},
                          "diagnostics":[]
                        },"meta":{},"error":null,"timestamp":"2026-05-29T10:00:00Z"}""",
                        MediaType.APPLICATION_JSON));

        RoutingManifest manifest = adapter.publishManifest();

        assertThat(manifest).isNotNull();
        assertThat(manifest.version()).isEqualTo(6);
        assertThat(manifest.payload()).isNotNull();
        assertThat(manifest.payload().get("upstreams").get("stub").get("url").asText())
                .isEqualTo("http://stub.local");
        server.verify();
    }
}
