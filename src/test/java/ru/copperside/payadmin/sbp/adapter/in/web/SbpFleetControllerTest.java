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
import ru.copperside.payadmin.sbp.application.fleet.port.out.SbpFleetPort;
import ru.copperside.payadmin.sbp.domain.RouterFleet;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SbpFleetControllerTest.MockConfig.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class SbpFleetControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void listReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/routers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.up").value(1))
                .andExpect(jsonPath("$.data.routers[0].instanceId").value("i-1"))
                .andExpect(jsonPath("$.data.routers[0].status").value("UP"))
                .andExpect(jsonPath("$.data.routers[0].backends[0].url").value("http://a/api"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/routers"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {

        @Bean
        @Primary
        FakeSbpFleetPort fakeSbpFleetPort() {
            return new FakeSbpFleetPort();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(token,
                    Instant.parse("2026-06-16T08:00:00Z"), Instant.parse("2026-06-16T10:00:00Z"),
                    Map.of("alg", "none"), Map.of("sub", "alice"));
        }
    }

    static class FakeSbpFleetPort implements SbpFleetPort {
        @Override
        public RouterFleet listRouters() {
            return new RouterFleet(1, 1, List.of(new RouterFleet.RouterInstance(
                    "i-1", "UP", Instant.parse("2026-06-16T09:00:00Z"), Instant.parse("2026-06-16T10:00:00Z"),
                    "default", List.of("default"),
                    List.of(new RouterFleet.RouterBackend("http://a/api", "default", false)),
                    new RouterFleet.RouterMetrics(2, 10, 1, 20, 10, 40.5, 120))));
        }
    }
}
