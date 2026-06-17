package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.RoutingConfig;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpSbpRoutingConfigAdapterTest {

    private MockRestServiceServer server;
    private HttpSbpRoutingConfigAdapter adapter;

    private final SbpRouterManagementProperties properties = new SbpRouterManagementProperties(
            "http://sbp-mgmt:8087", "X-Internal-Admin-Key", "secret-key",
            Duration.ofSeconds(2), Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sbp-mgmt:8087");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpSbpRoutingConfigAdapter(properties, builder.build());
    }

    @Test
    void getForwardsAdminKeyAndUnwraps() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/routing-config")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Admin-Key", "secret-key"))
                .andRespond(withSuccess("""
                        {"data":{"version":3,"activeGroup":"default",
                          "groups":{"default":{"backends":["http://a/api"]}}},
                         "meta":{},"error":null,"timestamp":"2026-06-17T10:00:00Z"}""", MediaType.APPLICATION_JSON));

        RoutingConfig c = adapter.get();
        assertThat(c.version()).isEqualTo(3L);
        assertThat(c.activeGroup()).isEqualTo("default");
        assertThat(c.groups().get("default").backends()).containsExactly("http://a/api");
        server.verify();
    }

    @Test
    void putSendsBodyAndUnwrapsSaved() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/routing-config")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.activeGroup").value("default"))
                .andExpect(jsonPath("$.groups.default.backends[0]").value("http://a/api"))
                .andRespond(withSuccess("""
                        {"data":{"version":4,"activeGroup":"default",
                          "groups":{"default":{"backends":["http://a/api"]}}},
                         "meta":{},"error":null,"timestamp":"2026-06-17T10:00:00Z"}""", MediaType.APPLICATION_JSON));

        RoutingConfig saved = adapter.replace(new RoutingConfig(null, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://a/api")))));
        assertThat(saved.version()).isEqualTo(4L);
        server.verify();
    }

    @Test
    void invalidPutMappedToProblem() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/routing-config")))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"error":{"type":"https://contracts.newpay/errors/routing-config-invalid",
                                "title":"Routing config problem","status":400,"code":"ROUTING_CONFIG_INVALID",
                                "message":"x","details":null,"traceId":"t"},"timestamp":"2026-06-17T10:00:00Z"}"""));

        assertThatThrownBy(() -> adapter.replace(new RoutingConfig(null, "bad", Map.of())))
                .isInstanceOf(UpstreamProblemException.class)
                .satisfies(ex -> assertThat(((UpstreamProblemException) ex).statusCode().value()).isEqualTo(400));
        server.verify();
    }

    @Test
    void roundTripsAuthPayRoute() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/routing-config")))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.authPay.enabled").value(true))
                .andExpect(jsonPath("$.authPay.backends[0]").value("http://authpay/x"))
                .andRespond(withSuccess("""
                        {"data":{"version":5,"activeGroup":"default",
                          "groups":{"default":{"backends":["http://a/api"]}},
                          "authPay":{"enabled":true,"backends":["http://authpay/x"],"timeoutMs":1500}},
                         "meta":{},"error":null,"timestamp":"2026-06-17T10:00:00Z"}""", MediaType.APPLICATION_JSON));

        RoutingConfig saved = adapter.replace(new RoutingConfig(null, "default",
                Map.of("default", new RoutingConfig.Group(List.of("http://a/api"))),
                new RoutingConfig.AuthPay(true, List.of("http://authpay/x"), 1500)));

        assertThat(saved.authPay().enabled()).isTrue();
        assertThat(saved.authPay().backends()).containsExactly("http://authpay/x");
        assertThat(saved.authPay().timeoutMs()).isEqualTo(1500);
        server.verify();
    }
}
