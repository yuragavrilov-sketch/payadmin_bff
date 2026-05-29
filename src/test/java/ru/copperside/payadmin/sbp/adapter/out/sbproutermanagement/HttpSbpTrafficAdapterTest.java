package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.sbp.application.traffic.TrafficQuery;
import ru.copperside.payadmin.sbp.config.SbpRouterManagementProperties;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class HttpSbpTrafficAdapterTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private HttpSbpTrafficAdapter adapter;

    private final SbpRouterManagementProperties properties = new SbpRouterManagementProperties(
            "http://sbp-mgmt:8087", "X-Internal-Admin-Key", "secret-key",
            Duration.ofSeconds(2), Duration.ofSeconds(5));

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://sbp-mgmt:8087");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpSbpTrafficAdapter(properties, builder.build());
    }

    @Test
    void listForwardsFiltersAndUnwraps() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/traffic/transactions")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("requestType", "ReqAuthPay"))
                .andExpect(queryParam("page", "0"))
                .andRespond(withSuccess("""
                        {"data":{"items":[{"correlationId":"c1","txId":"tx1","requestType":"ReqAuthPay",
                        "terminalOwner":"o","route":"r","upstream":"infosrv","outcome":"ok","status":"RESPONDED",
                        "requestAt":"2026-05-29T09:00:00Z","responseAt":"2026-05-29T09:00:00.040Z","latencyMs":40,
                        "env":"compose"}],"total":1,"page":0,"size":50},"meta":{},"error":null,
                        "timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        TrafficListResult result = adapter.listTransactions(new TrafficQuery(
                "ReqAuthPay", null, null, null, null, null, null, null, 0, 50));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().get(0).correlationId()).isEqualTo("c1");
        server.verify();
    }

    @Test
    void statsUnwrapsNestedStats() {
        server.expect(requestTo(containsString("/internal/v1/sbp-router-management/traffic/stats")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"data":{"stats":{"total":3,"responded":2,"pending":1,"byOutcome":{"ok":2},
                        "byRequestType":{"ReqAuthPay":3},"byUpstream":{"infosrv":2},"latencyP95":50,"latencyP99":60,
                        "latencyAvg":40,"throughputPerMinute":[{"minute":"2026-05-29T09:00:00Z","count":3}]}},
                        "meta":{},"error":null,"timestamp":"2026-05-29T09:00:00Z"}""", MediaType.APPLICATION_JSON));

        TrafficStats stats = adapter.stats(null, null);

        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.byOutcome()).containsEntry("ok", 2L);
        server.verify();
    }

    @Test
    void notFoundIsMappedToProblem() {
        server.expect(requestToUriTemplate("http://sbp-mgmt:8087/internal/v1/sbp-router-management/traffic/transactions/{id}", "missing"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"error":{"type":"https://contracts.newpay/errors/traffic-transaction-not-found",
                                "title":"Traffic transaction problem","status":404,"code":"TRAFFIC_TRANSACTION_NOT_FOUND",
                                "message":"x","details":null,"traceId":"t"},"timestamp":"2026-05-29T09:00:00Z"}"""));

        assertThatThrownBy(() -> adapter.getTransaction("missing"))
                .isInstanceOf(UpstreamProblemException.class)
                .satisfies(ex -> assertThat(((UpstreamProblemException) ex).statusCode().value()).isEqualTo(404));
        server.verify();
    }
}
