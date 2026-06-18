package ru.copperside.payadmin.limitprojection.adapter.out.limitprojection;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.copperside.payadmin.limitprojection.config.LimitProjectionProperties;
import ru.copperside.payadmin.limitprojection.domain.ReservationState;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;
import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpReservationProjectionAdapterTest {

    private MockWebServer server;
    private HttpReservationProjectionAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        adapter = new HttpReservationProjectionAdapter(new LimitProjectionProperties(
                server.url("/").toString(), "X-Internal-Admin-Key", "test-key",
                Duration.ofSeconds(2), Duration.ofSeconds(5)));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void listForwardsParamsAndAdminKeyAndMapsItems() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"data":[{"reservationId":"r1","operationId":"op1","state":"CONFIRMED","merchantId":"502118",
                        "operationType":"SBP_C2B","direction":"IN","amount":"250.00","currency":"RUB",
                        "heldAt":"2026-06-17T12:00:00Z","lastOccurredAt":"2026-06-17T12:05:00Z","staleAfter":null}],
                        "meta":{},"error":null,"timestamp":"2026-06-17T12:06:00Z"}
                        """));

        ReservationStatePage page = adapter.list("502118", "CONFIRMED", "2026-06-17T00:00:00Z", "2026-06-18T00:00:00Z", 1, 50);

        assertThat(page.items()).hasSize(1);
        ReservationState first = page.items().getFirst();
        assertThat(first.reservationId()).isEqualTo("r1");
        assertThat(first.merchantId()).isEqualTo("502118");
        assertThat(first.amount()).isEqualTo("250.00");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(50);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("/internal/v1/limit-projection/reservations");
        assertThat(request.getPath()).contains("merchantId=502118");
        assertThat(request.getPath()).contains("state=CONFIRMED");
        assertThat(request.getPath()).contains("from=2026-06-17");
        assertThat(request.getPath()).contains("page=0");
        assertThat(request.getPath()).contains("size=50");
        assertThat(request.getHeader("X-Internal-Admin-Key")).isEqualTo("test-key");
    }

    @Test
    void summaryMapsRows() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"data":[{"groupKey":"502118","confirmedCount":2,"confirmedAmount":"750.00","currency":"RUB"}],
                        "meta":{},"error":null,"timestamp":"t"}
                        """));

        List<ReservationSummaryRow> rows = adapter.summary("502118", null, null, "merchant");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().confirmedCount()).isEqualTo(2);
        assertThat(rows.getFirst().confirmedAmount()).isEqualTo("750.00");
    }
}
