package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.RouterFleet;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpSbpFleetAdapterTest {

    private MockRestServiceServer server;
    private HttpSbpFleetAdapter adapter;

    private final SbpRouterManagementProperties properties = new SbpRouterManagementProperties(
            "http://sbp-mgmt:8087", "X-Internal-Admin-Key", "secret-key",
            Duration.ofSeconds(2), Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sbp-mgmt:8087");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpSbpFleetAdapter(properties, builder.build());
    }

    @Test
    void listRoutersForwardsAdminKeyAndUnwraps() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/routers")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Admin-Key", "secret-key"))
                .andRespond(withSuccess("""
                        {"data":{"total":2,"up":1,"routers":[
                          {"instanceId":"i-1","status":"UP","startedAt":"2026-06-16T09:00:00Z",
                           "lastHeartbeat":"2026-06-16T10:00:00Z","activeGroup":"default","groups":["default"],
                           "backends":[{"url":"http://a/api","group":"default","banned":false}],
                           "metrics":{"activeRequests":2,"requestsTotal":10,"upstreamErrorsTotal":1,
                                      "kafkaPublishedTotal":20,"requestCount":10,"avgLatencyMs":40.5,"maxLatencyMs":120}}]},
                         "meta":{},"error":null,"timestamp":"2026-06-16T10:00:00Z"}""", MediaType.APPLICATION_JSON));

        RouterFleet fleet = adapter.listRouters();

        assertThat(fleet.total()).isEqualTo(2);
        assertThat(fleet.up()).isEqualTo(1);
        RouterFleet.RouterInstance i = fleet.routers().get(0);
        assertThat(i.instanceId()).isEqualTo("i-1");
        assertThat(i.status()).isEqualTo("UP");
        // tolerant to version skew: an older management without the field unwraps to null, not a 500
        assertThat(i.routingConfigVersion()).isNull();
        assertThat(i.lastHeartbeat()).isEqualTo(Instant.parse("2026-06-16T10:00:00Z"));
        assertThat(i.backends().get(0).url()).isEqualTo("http://a/api");
        assertThat(i.metrics().activeRequests()).isEqualTo(2);
        assertThat(i.metrics().avgLatencyMs()).isEqualTo(40.5);
        server.verify();
    }
}
