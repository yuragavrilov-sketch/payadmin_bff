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
import ru.copperside.payadmin.sbp.application.routingconfig.port.out.SbpRoutingConfigPort;
import ru.copperside.payadmin.sbp.domain.RoutingConfig;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SbpRoutingConfigControllerTest.MockConfig.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class SbpRoutingConfigControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void getReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/routing-config")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(3))
                .andExpect(jsonPath("$.data.activeGroup").value("default"))
                .andExpect(jsonPath("$.data.groups.default.backends[0]").value("http://a/api"));
    }

    @Test
    void putForwardsConfigAndReturnsSaved() throws Exception {
        mockMvc.perform(put("/api/v1/sbp/routing-config")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read")))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"version":null,"activeGroup":"blue",
                                 "groups":{"blue":{"backends":["http://b/api"]}}}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(4))
                .andExpect(jsonPath("$.data.activeGroup").value("blue"))
                .andExpect(jsonPath("$.data.groups.blue.backends[0]").value("http://b/api"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/routing-config"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {

        @Bean
        @Primary
        FakeSbpRoutingConfigPort fakeSbpRoutingConfigPort() {
            return new FakeSbpRoutingConfigPort();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(token,
                    Instant.parse("2026-06-16T08:00:00Z"), Instant.parse("2026-06-16T10:00:00Z"),
                    Map.of("alg", "none"), Map.of("sub", "alice"));
        }
    }

    static class FakeSbpRoutingConfigPort implements SbpRoutingConfigPort {
        private final AtomicReference<RoutingConfig> stored = new AtomicReference<>(
                new RoutingConfig(3L, "default",
                        Map.of("default", new RoutingConfig.Group(List.of("http://a/api")))));

        @Override
        public RoutingConfig get() {
            return stored.get();
        }

        @Override
        public RoutingConfig replace(RoutingConfig config) {
            return new RoutingConfig(4L, config.activeGroup(), config.groups());
        }
    }
}
