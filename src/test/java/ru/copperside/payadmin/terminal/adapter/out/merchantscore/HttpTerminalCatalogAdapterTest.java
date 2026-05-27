package ru.copperside.payadmin.terminal.adapter.out.merchantscore;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.merchant.config.MerchantsCoreProperties;
import ru.copperside.payadmin.terminal.application.port.out.TerminalPage;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpTerminalCatalogAdapterTest {

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
    void fetchPageSendsParamsHeadersAndParsesBody() throws Exception {
        server.enqueue(json("""
                {
                  "data": [
                    { "mercId": 1, "mps": "VISA", "gate": "ECOM", "is3ds": true, "terminalId": "T1",
                      "merchantId": "M1", "mcc": "5411", "name": "Alpha VISA", "merchantUrl": "http://a",
                      "login": "login1", "hasPassword": true, "apiUrl": "http://api1", "merchantName": "Alpha Shop" }
                  ],
                  "meta": { "limit": 50, "offset": 0, "count": 1, "total": 4684 },
                  "error": null,
                  "timestamp": "2026-05-27T20:00:00Z"
                }
                """));
        MDC.put(RequestIdFilter.MDC_KEY, "00000000-0000-0000-0000-000000000001");

        TerminalPage page = client().fetchPage(50, 0, "visa", "mps", "desc");

        assertThat(page.total()).isEqualTo(4684L);
        assertThat(page.lines()).hasSize(1);
        assertThat(page.lines().get(0).mercId()).isEqualTo(1L);
        assertThat(page.lines().get(0).hasPassword()).isTrue();
        assertThat(page.lines().get(0).merchantName()).isEqualTo("Alpha Shop");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(
                "/api/v1/terminals?limit=50&offset=0&search=visa&sortBy=mps&sortDir=desc");
        assertThat(request.getHeader("X-Core-Key")).isEqualTo("secret");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void fetchPageOmitsSearchWhenNull() throws Exception {
        server.enqueue(json("""
                { "data": [], "meta": { "total": 0 }, "error": null, "timestamp": "2026-05-27T20:00:00Z" }
                """));

        client().fetchPage(100, 0, null, "mercId", "asc");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/v1/terminals?limit=100&offset=0&sortBy=mercId&sortDir=asc");
    }

    @Test
    void fetchByMerchantTargetsScopedPath() throws Exception {
        server.enqueue(json("""
                { "data": [], "meta": { "total": 0 }, "error": null, "timestamp": "2026-05-27T20:00:00Z" }
                """));

        client().fetchByMerchant(7L, 100, 0, null, "mercId", "asc");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(
                "/api/v1/merchants/7/terminals?limit=100&offset=0&sortBy=mercId&sortDir=asc");
    }

    @Test
    void upstreamErrorMapsToUnavailable() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));

        assertThatThrownBy(() -> client().fetchPage(100, 0, null, "mercId", "asc"))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessageContaining("merchants-core");
    }

    private HttpTerminalCatalogAdapter client() {
        return new HttpTerminalCatalogAdapter(properties());
    }

    private MerchantsCoreProperties properties() {
        return new MerchantsCoreProperties(
                server.url("/").toString(), "X-Core-Key", "secret",
                Duration.ofSeconds(1), Duration.ofSeconds(1), 500, 20);
    }

    private MockResponse json(String body) {
        return new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json").setBody(body);
    }
}
