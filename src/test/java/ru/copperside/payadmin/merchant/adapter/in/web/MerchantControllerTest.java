package ru.copperside.payadmin.merchant.adapter.in.web;

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
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationEntry;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(MerchantControllerTest.TestSupport.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class MerchantControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FakeMerchantCatalogPort merchantCatalogPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        merchantCatalogPort.clear()
                .withMerchant(184L, "ООО «Ромашка»", Map.of("MCC", "5411"))
                .withConfig(184L,
                        entry("NAME", "ООО «Ромашка»", "2025-02-04T10:00:00Z"),
                        entry("MCC", "5411", "2025-02-05T10:00:00Z")
                );

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void listMerchantsReturnsContractEnvelopeWithDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].id").value("MRC-00184"))
                .andExpect(jsonPath("$.data[0].name").value("ООО «Ромашка»"))
                .andExpect(jsonPath("$.data[0].status").value("active"))
                .andExpect(jsonPath("$.data[0].mcc").value("5411"))
                .andExpect(jsonPath("$.data[0].createdAt").value("2025-02-04T10:00:00Z"))
                .andExpect(jsonPath("$.meta.limit").value(100))
                .andExpect(jsonPath("$.meta.offset").value(0))
                .andExpect(jsonPath("$.meta.count").value(1))
                .andExpect(jsonPath("$.meta.search").value(nullValue()))
                .andExpect(jsonPath("$.meta.status").value(nullValue()))
                .andExpect(jsonPath("$.meta.sortBy").value("id"))
                .andExpect(jsonPath("$.meta.sortDir").value("asc"))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").value("2026-05-25T20:00:00Z"));
    }

    @Test
    void listMerchantsRejectsInvalidLimitAsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/merchants?limit=0")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    @Test
    void listMerchantsRejectsUnsupportedStatusAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/merchants?status=pending")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error.status").value(400));
    }

    private static MerchantConfigurationEntry entry(String name, String value, String dateBegin) {
        return new MerchantConfigurationEntry(
                name,
                value,
                Instant.parse(dateBegin),
                Instant.parse("2099-01-01T00:00:00Z")
        );
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
        FakeMerchantCatalogPort FakeMerchantCatalogPort() {
            return new FakeMerchantCatalogPort();
        }
    }

    static class FakeMerchantCatalogPort implements MerchantCatalogPort {

        private final List<MerchantConfigurationLine> merchants = new ArrayList<>();
        private final Map<Long, List<MerchantConfigurationEntry>> configByMerchant = new LinkedHashMap<>();

        FakeMerchantCatalogPort clear() {
            merchants.clear();
            configByMerchant.clear();
            return this;
        }

        FakeMerchantCatalogPort withMerchant(Long mercId, String name, Map<String, String> configuration) {
            merchants.add(new MerchantConfigurationLine(mercId, name, configuration));
            return this;
        }

        FakeMerchantCatalogPort withConfig(Long merchantId, MerchantConfigurationEntry... entries) {
            configByMerchant.put(merchantId, List.of(entries));
            return this;
        }

        @Override
        public List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir) {
            return merchants.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId) {
            return configByMerchant.getOrDefault(merchantId, List.of());
        }
    }
}


