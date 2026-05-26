package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationEntry;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.merchant.config.MerchantsCoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMerchantsCoreAdapterTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        MDC.clear();
        server.shutdown();
    }

    @Test
    void fetchActiveLinesSendsQueryParamsAndInternalHeaders() throws Exception {
        server.enqueue(json("""
                {
                  "data": [
                    {
                      "mercId": 184,
                      "name": "ООО Ромашка",
                      "hierarchyId": 10,
                      "initiator": "Alice",
                      "circuit": "PAY",
                      "configuration": { "MCC": "5411" },
                      "activeSince": "2020-01-01T00:00:00Z"
                    }
                  ],
                  "meta": { "count": 1 },
                  "error": null,
                  "timestamp": "2026-05-25T20:00:00Z"
                }
                """));
        MDC.put(RequestIdFilter.MDC_KEY, "00000000-0000-0000-0000-000000000001");

        List<MerchantConfigurationLine> merchants = client().fetchActiveLines(3, 5, null, "mercId", "asc");

        assertThat(merchants).containsExactly(new MerchantConfigurationLine(
                184L,
                "ООО Ромашка",
                java.util.Map.of("MCC", "5411"),
                Instant.parse("2020-01-01T00:00:00Z")
        ));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/merchants/configurations/active-line?limit=3&offset=5&sortBy=mercId&sortDir=asc");
        assertThat(request.getHeader("X-Core-Key")).isEqualTo("secret");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void fetchActiveConfigurationMapsEntries() {
        server.enqueue(json("""
                {
                  "data": [
                    {
                      "parameterName": "MCC",
                      "parameterValue": "5411",
                      "dateBegin": "2025-02-04T10:00:00Z",
                      "dateEnd": "2099-01-01T00:00:00Z"
                    }
                  ],
                  "meta": { "count": 1 },
                  "error": null,
                  "timestamp": "2026-05-25T20:00:00Z"
                }
                """));

        List<MerchantConfigurationEntry> entries = client().fetchActiveConfiguration(184L);

        assertThat(entries).containsExactly(new MerchantConfigurationEntry(
                "MCC",
                "5411",
                Instant.parse("2025-02-04T10:00:00Z"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));
    }

    @Test
    void upstreamServerErrorMapsToUnavailableException() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));

        assertThatThrownBy(() -> client().fetchActiveLines(100, 0, null, "mercId", "asc"))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessageContaining("merchants-core");
    }

    @Test
    void countActiveLinesParsesTotalAndSendsHeaders() throws Exception {
        server.enqueue(json("""
                {
                  "data": { "total": 4572 },
                  "error": null,
                  "timestamp": "2026-05-25T20:00:00Z"
                }
                """));
        MDC.put(RequestIdFilter.MDC_KEY, "00000000-0000-0000-0000-000000000042");

        long total = client().countActiveLines(null);

        assertThat(total).isEqualTo(4572L);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/merchants/configurations/active-line/count");
        assertThat(request.getHeader("X-Core-Key")).isEqualTo("secret");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("00000000-0000-0000-0000-000000000042");
    }

    @Test
    void countActiveLinesForwardsSearchWhenPresent() throws Exception {
        server.enqueue(json("""
                { "data": { "total": 1 }, "error": null, "timestamp": "2026-05-25T20:00:00Z" }
                """));

        long total = client().countActiveLines("market");

        assertThat(total).isEqualTo(1L);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/merchants/configurations/active-line/count?search=market");
    }

    @Test
    void countActiveLinesMapsUpstreamErrorToUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));

        assertThatThrownBy(() -> client().countActiveLines(null))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessageContaining("merchants-core");
    }

    private HttpMerchantsCoreAdapter client() {
        return new HttpMerchantsCoreAdapter(properties());
    }

    private MerchantsCoreProperties properties() {
        return new MerchantsCoreProperties(
                server.url("/").toString(),
                "X-Core-Key",
                "secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                500,
                20
        );
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}

