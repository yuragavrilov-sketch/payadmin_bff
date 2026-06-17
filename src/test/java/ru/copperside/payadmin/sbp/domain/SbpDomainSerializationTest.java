package ru.copperside.payadmin.sbp.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SbpDomainSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void trafficTransactionSummaryOmitsNullXml() throws Exception {
        TrafficTransaction t = new TrafficTransaction("c1", "tx1", "ReqAuthPay", "owner", "route",
                "infosrv", "ok", "RESPONDED", null, null, 40L, "compose", null, null, null, null);
        String json = mapper.writeValueAsString(t);
        assertThat(json).doesNotContain("requestXml").doesNotContain("responseXml");
    }
}
