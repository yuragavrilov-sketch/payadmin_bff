package ru.copperside.payadmin.limitprojection.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.copperside.payadmin.limitprojection.application.port.out.ReservationProjectionPort;
import ru.copperside.payadmin.limitprojection.domain.ReservationEvent;
import ru.copperside.payadmin.limitprojection.domain.ReservationState;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;
import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(LimitReservationControllerTest.TestSupport.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class LimitReservationControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // 1. GET list → 200, data[0].reservationId == "r1", meta.limit == 50, meta.offset == 0, meta.count == 1
    @Test
    void listReturnsPageWithMeta() throws Exception {
        mockMvc.perform(get("/api/v1/limit-reservations?merchantId=502118&page=1&size=50")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].reservationId").value("r1"))
                .andExpect(jsonPath("$.meta.limit").value(50))
                .andExpect(jsonPath("$.meta.offset").value(0))
                .andExpect(jsonPath("$.meta.count").value(1));
    }

    // 2. GET /r1 (present) → 200, data.state == "CONFIRMED"
    @Test
    void getByIdReturnsReservation() throws Exception {
        mockMvc.perform(get("/api/v1/limit-reservations/r1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.state").value("CONFIRMED"));
    }

    // 3. GET /missing (empty) → 404 problem+json
    @Test
    void getByIdNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/limit-reservations/missing")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // 4. GET /summary → 200, data[0].confirmedAmount == "750.00"
    @Test
    void summaryReturnsSummaryRow() throws Exception {
        mockMvc.perform(get("/api/v1/limit-reservations/summary?merchantId=502118&groupBy=merchant")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].confirmedAmount").value("750.00"));
    }

    // 5. No JWT authority → 401/403
    @Test
    void requestWithoutAuthorityIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/limit-reservations"))
                .andExpect(status().is(org.hamcrest.Matchers.either(
                        org.hamcrest.Matchers.is(401)).or(org.hamcrest.Matchers.is(403))));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-05-25T20:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(
                    token,
                    Instant.parse("2026-05-25T19:00:00Z"),
                    Instant.parse("2026-05-25T21:00:00Z"),
                    Map.of("alg", "none"),
                    Map.of("sub", "alice")
            );
        }

        @Bean
        @Primary
        FakeReservationProjectionPort fakeReservationProjectionPort() {
            return new FakeReservationProjectionPort();
        }
    }

    static class FakeReservationProjectionPort implements ReservationProjectionPort {

        private static final ReservationState SAMPLE_STATE = new ReservationState(
                "r1", "op1", "CONFIRMED", "502118",
                "PAYMENT", "DEBIT", "500.00", "RUB",
                "2026-01-01T10:00:00Z", "2026-01-01T10:05:00Z", null
        );

        @Override
        public ReservationStatePage list(String merchantId, String state, String from, String to, int page, int size) {
            return new ReservationStatePage(List.of(SAMPLE_STATE), page, size);
        }

        @Override
        public Optional<ReservationState> findByReservationId(String reservationId) {
            if ("r1".equals(reservationId)) {
                return Optional.of(SAMPLE_STATE);
            }
            return Optional.empty();
        }

        @Override
        public Optional<ReservationState> findByOperationId(String operationId) {
            return Optional.empty();
        }

        @Override
        public List<ReservationEvent> events(String reservationId) {
            return List.of();
        }

        @Override
        public List<ReservationSummaryRow> summary(String merchantId, String from, String to, String groupBy) {
            return List.of(new ReservationSummaryRow("502118", 2, "750.00", "RUB"));
        }
    }
}
