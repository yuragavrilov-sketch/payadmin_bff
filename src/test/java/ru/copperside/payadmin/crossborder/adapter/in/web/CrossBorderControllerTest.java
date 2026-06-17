package ru.copperside.payadmin.crossborder.adapter.in.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import ru.copperside.payadmin.crossborder.application.port.out.CrossBorderEnginePort;
import ru.copperside.payadmin.crossborder.domain.CrossBorderOperation;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(CrossBorderControllerTest.TestSupport.class)
@TestPropertySource(properties = {
        "payadmin-bff.security.required-authority=payadmin.read"
})
class CrossBorderControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CrossBorderEnginePort enginePort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        reset(enginePort);
    }

    @Test
    void getBanksReturnsNestedDataEnvelope() throws Exception {
        when(enginePort.listBanks()).thenReturn(List.of(
                new PartnerCountry("AZ", "Азербайджан", "Azerbaijan", List.of())));

        mockMvc.perform(get("/api/v1/crossborder/banks")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].countryCode").value("AZ"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

        verify(enginePort).listBanks();
    }

    @Test
    void getOperationsPassesLimitOffsetAndBuildsMeta() throws Exception {
        when(enginePort.listOperations(10, 5)).thenReturn(new OperationsPage(List.of(), 7L));

        mockMvc.perform(get("/api/v1/crossborder/operations?limit=10&offset=5")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.limit").value(10))
                .andExpect(jsonPath("$.meta.offset").value(5))
                .andExpect(jsonPath("$.meta.total").value(7));

        verify(enginePort).listOperations(10, 5);
    }

    @Test
    void getSettingsReturnsData() throws Exception {
        when(enginePort.getSettings()).thenReturn(new TransferSettings(
                17L, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("100"),
                "RUB", "USD", "card", true, Instant.parse("2026-06-17T20:00:00Z"), "flyway-seed"));

        mockMvc.perform(get("/api/v1/crossborder/settings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultSenderCurrency").value("RUB"));
    }

    @Test
    void putSettingsMapsRequestToDomainAndReturnsUpdated() throws Exception {
        ArgumentCaptor<TransferSettingsUpdate> captor = ArgumentCaptor.forClass(TransferSettingsUpdate.class);
        when(enginePort.updateSettings(any())).thenReturn(new TransferSettings(
                42L, new BigDecimal("2.5"), BigDecimal.ONE, new BigDecimal("150"),
                "RUB", "EUR", "card", false, Instant.parse("2026-06-17T21:00:00Z"), "payadmin-bff"));

        mockMvc.perform(put("/api/v1/crossborder/settings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"walletId":42,"ratePercent":2.5,"senderPercent":1,"senderMinCommission":150,
                                 "defaultSenderCurrency":"RUB","defaultReceiverCurrency":"EUR","defaultPayoutMethod":"card","testMode":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultReceiverCurrency").value("EUR"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));

        verify(enginePort).updateSettings(captor.capture());
        assertThat(captor.getValue().defaultSenderCurrency()).isEqualTo("RUB");
        assertThat(captor.getValue().defaultReceiverCurrency()).isEqualTo("EUR");
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
        CrossBorderEnginePort crossBorderEnginePort() {
            return mock(CrossBorderEnginePort.class);
        }
    }
}
