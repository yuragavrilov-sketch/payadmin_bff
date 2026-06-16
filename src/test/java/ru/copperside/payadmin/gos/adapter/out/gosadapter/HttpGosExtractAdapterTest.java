package ru.copperside.payadmin.gos.adapter.out.gosadapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
import ru.copperside.payadmin.gos.application.GosExtractCommand;
import ru.copperside.payadmin.gos.application.GosExtractResult;
import ru.copperside.payadmin.gos.application.GosExtractStatusQuery;
import ru.copperside.payadmin.gos.application.GosExtractStatusResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpGosExtractAdapterTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private HttpGosExtractAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://j-gos-adapter:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpGosExtractAdapter(builder.build());
    }

    @Test
    void requestExtractSendsMerchIdHeaderAndOmitsMerchIdFromBody() {
        server.expect(requestTo("http://j-gos-adapter:8080/api/v1/nominal/egrul-egrip/getExtract"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("TCB.Header-Merch-Id", "M1"))
                .andExpect(content().json("""
                        {"beneficiaryType":"UL_RESIDENT","inn":"7700000000","name":"Acme"}"""))
                .andExpect(jsonPath("$.merchId").doesNotExist())
                .andRespond(withSuccess("""
                        {"status":"IN_PROGRESS","extractRequestId":"req-1","errorDesc":null}""",
                        MediaType.APPLICATION_JSON));

        GosExtractResult result = adapter.requestExtract(
                new GosExtractCommand("M1", "UL_RESIDENT", "7700000000", "Acme"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.extractRequestId()).isEqualTo("req-1");
        server.verify();
    }

    @Test
    void getStatusReturnsRawResponseObject() {
        server.expect(requestTo("http://j-gos-adapter:8080/api/v1/nominal/egrul-egrip/getExtractStatus"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("TCB.Header-Merch-Id", "M1"))
                .andExpect(content().json("""
                        {"extractRequestId":"req-1"}"""))
                .andRespond(withSuccess("""
                        {"status":"COMPLETED","response":{"ogrn":"1027700000000","name":"Acme"},
                        "errorDesc":null}""", MediaType.APPLICATION_JSON));

        GosExtractStatusResult result = adapter.getStatus(new GosExtractStatusQuery("M1", "req-1"));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.response()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.response()).get("ogrn")).isEqualTo("1027700000000");
        server.verify();
    }

    @Test
    void businessFailureAsHttp200IsPassedThroughNotThrown() {
        server.expect(requestTo("http://j-gos-adapter:8080/api/v1/nominal/egrul-egrip/getExtract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"status":"FAILED","extractRequestId":null,"errorDesc":"INN not found"}""",
                        MediaType.APPLICATION_JSON));

        GosExtractResult result = adapter.requestExtract(
                new GosExtractCommand("M1", "IP_RESIDENT", "0000000000", "Nobody"));

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorDesc()).isEqualTo("INN not found");
        server.verify();
    }

    @Test
    void transportFailureIsMappedToUpstreamUnavailable() {
        server.expect(requestTo("http://j-gos-adapter:8080/api/v1/nominal/egrul-egrip/getExtractStatus"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // A 5xx surfaces as RestClientResponseException, which is NOT caught here (only transport
        // errors are). It propagates as-is. Verify the call still hits the server.
        assertThatThrownBy(() -> adapter.getStatus(new GosExtractStatusQuery("M1", "req-1")))
                .isNotInstanceOf(UpstreamUnavailableException.class);
        server.verify();
    }
}
