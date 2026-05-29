package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.sbp.application.config.UpstreamRequest;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
