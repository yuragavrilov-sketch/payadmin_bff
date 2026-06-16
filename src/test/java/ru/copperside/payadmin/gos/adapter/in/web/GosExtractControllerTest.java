package ru.copperside.payadmin.gos.adapter.in.web;

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
import ru.copperside.payadmin.gos.application.GosExtractCommand;
import ru.copperside.payadmin.gos.application.GosExtractResult;
import ru.copperside.payadmin.gos.application.GosExtractStatusQuery;
import ru.copperside.payadmin.gos.application.GosExtractStatusResult;
import ru.copperside.payadmin.gos.application.port.out.GosExtractPort;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(GosExtractControllerTest.MockConfig.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class GosExtractControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FakeGosExtractPort fakePort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fakePort.reset();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getExtractForwardsBodyWithoutMerchIdAndReturnsEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/gos/egrul-egrip/getExtract")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchId\":\"M1\",\"beneficiaryType\":\"UL_RESIDENT\","
                                + "\"inn\":\"7700000000\",\"name\":\"Acme\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.extractRequestId").value("req-1"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void getExtractStatusReturnsRawResponseObject() throws Exception {
        mockMvc.perform(post("/api/v1/gos/egrul-egrip/getExtractStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchId\":\"M1\",\"extractRequestId\":\"req-1\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.response.ogrn").value("1027700000000"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {

        @Bean
        @Primary
        FakeGosExtractPort fakeGosExtractPort() {
            return new FakeGosExtractPort();
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

    static class FakeGosExtractPort implements GosExtractPort {

        GosExtractCommand lastCommand;
        GosExtractStatusQuery lastQuery;

        void reset() {
            lastCommand = null;
            lastQuery = null;
        }

        @Override
        public GosExtractResult requestExtract(GosExtractCommand command) {
            this.lastCommand = command;
            return new GosExtractResult("IN_PROGRESS", "req-1", null);
        }

        @Override
        public GosExtractStatusResult getStatus(GosExtractStatusQuery query) {
            this.lastQuery = query;
            return new GosExtractStatusResult("COMPLETED", Map.of("ogrn", "1027700000000"), null);
        }
    }
}
