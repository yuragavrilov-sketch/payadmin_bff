package ru.copperside.payadmin.crossborder.adapter.out.transgranengine;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.crossborder.config.TransgranEngineProperties;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpTransgranEngineAdapterTest {

    private MockWebServer server;
    private HttpTransgranEngineAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        adapter = new HttpTransgranEngineAdapter(new TransgranEngineProperties(
                server.url("/").toString(), "X-Internal-Admin-Key", "secret",
                Duration.ofSeconds(2), Duration.ofSeconds(5)));
    }

    @AfterEach
    void tearDown() throws Exception {
        MDC.clear();
        server.shutdown();
    }

    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    void listBanksMapsNestedTreeAndSendsInternalHeader() throws Exception {
        MDC.put(RequestIdFilter.MDC_KEY, "req-42");
        server.enqueue(json("""
            {"data":[
              {"countryCode":"AZ","countryName":"Азербайджан","countryNameEn":"Azerbaijan",
               "providers":[{"providerId":"all","providerName":"Все банки","providerNameEn":"All",
                 "methods":[{"method":"card","walletId":16774,"walletCurrency":"USD",
                   "supportedCurrencies":["AZN"],
                   "requiredFields":[{"name":"receiver.first_name","type":"string","description":"Имя","required":true}]}]}]}
            ],"timestamp":"2026-06-17T20:00:00Z"}"""));

        List<PartnerCountry> banks = adapter.listBanks();

        assertThat(banks).hasSize(1);
        assertThat(banks.get(0).countryCode()).isEqualTo("AZ");
        assertThat(banks.get(0).providers().get(0).methods().get(0).walletId()).isEqualTo(16774L);
        assertThat(banks.get(0).providers().get(0).methods().get(0).supportedCurrencies()).containsExactly("AZN");
        assertThat(banks.get(0).providers().get(0).methods().get(0).requiredFields().get(0).required()).isTrue();

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/internal/admin/transgran/banks");
        assertThat(recorded.getHeader("X-Internal-Admin-Key")).isEqualTo("secret");
        assertThat(recorded.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("req-42");
    }

    @Test
    void listBanksThrowsUpstreamUnavailableOn503() {
        server.enqueue(new MockResponse().setResponseCode(503));
        assertThatThrownBy(() -> adapter.listBanks()).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void listOperationsMapsDataAndTotalFromMeta() throws Exception {
        server.enqueue(json("""
            {"data":[{"id":"11111111-1111-1111-1111-111111111111","requestId":"req-1","type":"CURRENCY_CONVERT",
              "status":"OK","stage":"INITIATED","senderCurrency":"RUB","senderAmount":733.42,"receiverCurrency":"USD","receiverAmount":9.9,
              "ratePercent":1,"payoutMethod":"card","payoutType":null,"walletId":17,"isTest":true,
              "expiredDate":"2026-05-14T17:46:21+03:00","createdAt":"2026-06-17T20:00:00Z"}],
             "meta":{"limit":50,"offset":0,"count":1,"total":7},"timestamp":"2026-06-17T20:00:00Z"}"""));

        var page = adapter.listOperations(50, 0);

        assertThat(page.total()).isEqualTo(7L);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().get(0).requestId()).isEqualTo("req-1");
        assertThat(page.data().get(0).stage()).isEqualTo("INITIATED");
        assertThat(page.data().get(0).senderAmount()).isEqualByComparingTo("733.42");
        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/internal/admin/transgran/operations?limit=50&offset=0");
    }

    @Test
    void getSettingsMapsData() throws Exception {
        server.enqueue(json("""
            {"data":{"walletId":17,"ratePercent":1,"senderPercent":1,"senderMinCommission":100,
              "defaultSenderCurrency":"RUB","defaultReceiverCurrency":"USD","defaultPayoutMethod":"card",
              "testMode":true,"updatedAt":"2026-06-17T20:00:00Z","updatedBy":"flyway-seed"},
             "timestamp":"2026-06-17T20:00:00Z"}"""));

        TransferSettings s = adapter.getSettings();

        assertThat(s.defaultSenderCurrency()).isEqualTo("RUB");
        assertThat(s.testMode()).isTrue();
        assertThat(server.takeRequest().getPath()).isEqualTo("/internal/admin/transgran/settings");
    }

    @Test
    void updateSettingsPutsBodyAndReturnsUpdated() throws Exception {
        server.enqueue(json("""
            {"data":{"walletId":42,"ratePercent":2.5,"senderPercent":1,"senderMinCommission":150,
              "defaultSenderCurrency":"RUB","defaultReceiverCurrency":"EUR","defaultPayoutMethod":"card",
              "testMode":false,"updatedAt":"2026-06-17T21:00:00Z","updatedBy":"payadmin-bff"},
             "timestamp":"2026-06-17T21:00:00Z"}"""));

        TransferSettings updated = adapter.updateSettings(new ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate(
                42L, new java.math.BigDecimal("2.5"), java.math.BigDecimal.ONE, new java.math.BigDecimal("150"),
                "RUB", "EUR", "card", false));

        assertThat(updated.walletId()).isEqualTo(42L);
        assertThat(updated.defaultReceiverCurrency()).isEqualTo("EUR");
        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("PUT");
        assertThat(recorded.getPath()).isEqualTo("/internal/admin/transgran/settings");
        assertThat(recorded.getBody().readUtf8()).contains("\"defaultReceiverCurrency\":\"EUR\"");
    }

    @Test
    void proxyPayoutPostsToEnginePath_passthroughBodyAndResponse() throws Exception {
        server.enqueue(json("{\"request_id\":\"abc\",\"sender_amount\":733.42}"));
        var body = new tools.jackson.databind.ObjectMapper()
                .readTree("{\"wallet_id\":17,\"sender_amount\":\"733.42\"}");

        var result = adapter.proxyPayout("convert", body);

        assertThat(result.path("status").asInt()).isEqualTo(200);
        assertThat(result.path("body").path("request_id").asText()).isEqualTo("abc");
        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/payout/currency/convert");
        assertThat(recorded.getHeader("X-Internal-Admin-Key")).isEqualTo("secret");
        assertThat(recorded.getBody().readUtf8()).contains("\"wallet_id\":17");
    }

    @Test
    void proxyPayoutSurfacesUpstream4xx_insteadOf503() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"invalid_currency\"}"));

        var result = adapter.proxyPayout("create",
                new tools.jackson.databind.ObjectMapper().createObjectNode());

        assertThat(result.path("status").asInt()).isEqualTo(422);
        assertThat(result.path("body").path("error").asText()).isEqualTo("invalid_currency");
    }

    @Test
    void proxyPayoutUnknownOp_throwsIllegalArgument() {
        assertThatThrownBy(() -> adapter.proxyPayout("bogus",
                new tools.jackson.databind.ObjectMapper().createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
