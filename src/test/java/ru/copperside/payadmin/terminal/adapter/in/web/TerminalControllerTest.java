package ru.copperside.payadmin.terminal.adapter.in.web;

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
import ru.copperside.payadmin.terminal.application.port.out.TerminalCatalogPort;
import ru.copperside.payadmin.terminal.application.port.out.TerminalLine;
import ru.copperside.payadmin.terminal.application.port.out.TerminalPage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TerminalControllerTest.TestSupport.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class TerminalControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void listTerminalsReturnsEnvelopeWithMaskedPassword() throws Exception {
        mockMvc.perform(get("/api/v1/terminals")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.data[0].mps").value("VISA"))
                .andExpect(jsonPath("$.data[0].is3ds").value(true))
                .andExpect(jsonPath("$.data[0].hasPassword").value(true))
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].merchantName").value("Alpha Shop"))
                .andExpect(jsonPath("$.meta.total").value(4))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void merchantTerminalsEndpointReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/1/terminals")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mercId").value(1))
                .andExpect(jsonPath("$.meta.total").value(4));
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/terminals?limit=0")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(token,
                    Instant.parse("2026-05-27T19:00:00Z"),
                    Instant.parse("2026-05-27T21:00:00Z"),
                    Map.of("alg", "none"), Map.of("sub", "alice"));
        }

        @Bean
        @Primary
        TerminalCatalogPort fakeTerminalCatalogPort() {
            return new TerminalCatalogPort() {
                @Override
                public TerminalPage fetchPage(int limit, int offset, String search, String sortBy, String sortDir) {
                    return new TerminalPage(List.of(new TerminalLine(
                            1L, "VISA", "ECOM", true, "T1", "M1", "5411", "Alpha VISA",
                            "http://a", "login1", true, "http://api1", "Alpha Shop")), 4L);
                }

                @Override
                public TerminalPage fetchByMerchant(long mercId, int limit, int offset, String search, String sortBy, String sortDir) {
                    return fetchPage(limit, offset, search, sortBy, sortDir);
                }
            };
        }
    }
}
