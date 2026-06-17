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
}
