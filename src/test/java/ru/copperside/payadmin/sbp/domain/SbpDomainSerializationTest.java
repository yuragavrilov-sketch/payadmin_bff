package ru.copperside.payadmin.sbp.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SbpDomainSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fieldBindingOmitsNulls() throws Exception {
        String json = mapper.writeValueAsString(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null));
        assertThat(json).contains("\"name\":\"rcvTspId\"").doesNotContain("\"path\"");
    }

    @Test
    void trafficTransactionSummaryOmitsNullXml() throws Exception {
        TrafficTransaction t = new TrafficTransaction("c1", "tx1", "ReqAuthPay", "owner", "route",
                "infosrv", "ok", "RESPONDED", null, null, 40L, "compose", null, null);
        String json = mapper.writeValueAsString(t);
        assertThat(json).doesNotContain("requestXml").doesNotContain("responseXml");
    }

    @Test
    void roundTripExtractionRule() throws Exception {
        ExtractionRule rule = new ExtractionRule(java.util.UUID.randomUUID(), "ReqAuthPay",
                List.of(new FieldBinding("rcvTspId", "PayProfile", "RcvTSPId", null)), List.of(),
                "DRAFT", false, 1);
        ExtractionRule back = mapper.readValue(mapper.writeValueAsString(rule), ExtractionRule.class);
        assertThat(back.routingFields().get(0).name()).isEqualTo("rcvTspId");
    }
}
