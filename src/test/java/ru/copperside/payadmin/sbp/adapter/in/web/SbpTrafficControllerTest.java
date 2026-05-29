package ru.copperside.payadmin.sbp.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.copperside.payadmin.sbp.application.traffic.TrafficQuery;
import ru.copperside.payadmin.sbp.application.traffic.port.out.SbpTrafficPort;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;
import ru.copperside.payadmin.sbp.domain.TrafficTransaction;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SbpTrafficControllerTest.MockConfig.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class SbpTrafficControllerTest {

    private static final Instant NOW = Instant.parse("2026-05-29T09:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FakeSbpTrafficPort fakePort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void listReturnsEnvelopeAndForwardsFilters() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/traffic/transactions")
                        .param("requestType", "ReqAuthPay")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].correlationId").value("c1"))
                .andExpect(jsonPath("$.data.items[0].requestXml").doesNotExist());
    }

    @Test
    void getTransactionReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/traffic/transactions/c1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correlationId").value("c1"));
    }

    @Test
    void statsReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/traffic/stats")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {

        @Bean
        @Primary
        FakeSbpTrafficPort fakeSbpTrafficPort() {
            return new FakeSbpTrafficPort();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(
                    token,
                    Instant.parse("2026-05-29T08:00:00Z"),
                    Instant.parse("2026-05-29T10:00:00Z"),
                    Map.of("alg", "none"),
                    Map.of("sub", "alice")
            );
        }
    }

    static class FakeSbpTrafficPort implements SbpTrafficPort {

        private static final Instant NOW = Instant.parse("2026-05-29T09:00:00Z");

        @Override
        public TrafficListResult listTransactions(TrafficQuery query) {
            TrafficTransaction t = new TrafficTransaction(
                    "c1", "tx1", "ReqAuthPay", "owner", "route", "infosrv", "ok",
                    "RESPONDED", NOW, NOW.plusMillis(40), 40L, "compose", null, null);
            return new TrafficListResult(List.of(t), 1, 0, 50);
        }

        @Override
        public TrafficTransaction getTransaction(String correlationId) {
            return new TrafficTransaction(
                    correlationId, "tx1", "ReqAuthPay", "owner", "route", "infosrv", "ok",
                    "RESPONDED", NOW, NOW.plusMillis(40), 40L, "compose",
                    "<Req/>", "<Resp/>");
        }

        @Override
        public TrafficStats stats(Instant from, Instant to) {
            return new TrafficStats(
                    3L, 2L, 1L,
                    Map.of("ok", 2L), Map.of("ReqAuthPay", 3L), Map.of("infosrv", 2L),
                    50L, 60L, 40L, List.of());
        }
    }
}
