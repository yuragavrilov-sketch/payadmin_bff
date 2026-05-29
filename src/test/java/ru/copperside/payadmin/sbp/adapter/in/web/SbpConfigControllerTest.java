package ru.copperside.payadmin.sbp.adapter.in.web;

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
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.web.ProblemDetail;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.sbp.application.config.ExtractionRuleRequest;
import ru.copperside.payadmin.sbp.application.config.RoutingFlagRequest;
import ru.copperside.payadmin.sbp.application.config.TerminalConfigRequest;
import ru.copperside.payadmin.sbp.application.config.TkbPayEntryRequest;
import ru.copperside.payadmin.sbp.application.config.UpstreamRequest;
import ru.copperside.payadmin.sbp.application.config.port.out.SbpConfigPort;
import ru.copperside.payadmin.sbp.domain.ExtractionRule;
import ru.copperside.payadmin.sbp.domain.PendingChanges;
import ru.copperside.payadmin.sbp.domain.RoutingFlag;
import ru.copperside.payadmin.sbp.domain.RoutingManifest;
import ru.copperside.payadmin.sbp.domain.TerminalConfig;
import ru.copperside.payadmin.sbp.domain.TkbPayEntry;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(SbpConfigControllerTest.MockConfig.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class SbpConfigControllerTest {

    private static final UUID UPSTREAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-05-29T09:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FakeSbpConfigPort fakePort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fakePort.reset();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void listUpstreamsReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/sbp/upstreams")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("infosrv"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void createUpstreamForwardsBody() throws Exception {
        mockMvc.perform(post("/api/v1/sbp/upstreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"infosrv\",\"url\":\"http://u\"}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void removeUpstreamMarksRemoval() throws Exception {
        mockMvc.perform(delete("/api/v1/sbp/upstreams/" + UPSTREAM_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.removal").value(true));
    }

    @Test
    void removeExtractionRuleMarksRemoval() throws Exception {
        mockMvc.perform(delete("/api/v1/sbp/extraction-rules/" + UPSTREAM_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.removal").value(true));
    }

    @Test
    void publishConflictIsRenderedAsProblem() throws Exception {
        fakePort.publishShouldThrowConflict = true;

        mockMvc.perform(post("/api/v1/sbp/routing-manifests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROUTING_MANIFEST_CONFLICT"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfig {

        @Bean
        @Primary
        FakeSbpConfigPort fakeSbpConfigPort() {
            return new FakeSbpConfigPort();
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

    static class FakeSbpConfigPort implements SbpConfigPort {

        boolean publishShouldThrowConflict = false;

        void reset() {
            publishShouldThrowConflict = false;
        }

        @Override
        public List<Upstream> listUpstreams() {
            return List.of(new Upstream(UPSTREAM_ID, "infosrv", "http://u", 30000, 2, 500, "ACTIVE", false, 1, NOW));
        }

        @Override
        public Upstream createUpstream(UpstreamRequest request) {
            return new Upstream(UPSTREAM_ID, request.name(), request.url(), null, null, null, "DRAFT", false, 1, NOW);
        }

        @Override
        public Upstream patchUpstream(UUID id, UpstreamRequest request) {
            return new Upstream(id, request.name() != null ? request.name() : "infosrv",
                    request.url() != null ? request.url() : "http://u", null, null, null, "ACTIVE", false, 2, NOW);
        }

        @Override
        public Upstream removeUpstream(UUID id) {
            return new Upstream(id, "infosrv", "http://u", null, null, null, "REMOVED", true, 2, NOW);
        }

        @Override
        public List<ExtractionRule> listExtractionRules() {
            return List.of();
        }

        @Override
        public ExtractionRule createExtractionRule(ExtractionRuleRequest request) {
            return new ExtractionRule(UUID.randomUUID(), request.messageType(), List.of(), List.of(), "DRAFT", false, 1);
        }

        @Override
        public ExtractionRule patchExtractionRule(UUID id, ExtractionRuleRequest request) {
            return new ExtractionRule(id, "ReqAuthPay", List.of(), List.of(), "DRAFT", false, 2);
        }

        @Override
        public ExtractionRule removeExtractionRule(UUID id) {
            return new ExtractionRule(id, "ReqAuthPay", List.of(), List.of(), "REMOVED", true, 2);
        }

        @Override
        public TerminalConfig getTerminalConfig() {
            return new TerminalConfig(UUID.randomUUID(), "rcvTspId", "b2cField", "TKB", "ACTIVE", 1);
        }

        @Override
        public TerminalConfig putTerminalConfig(TerminalConfigRequest request) {
            return new TerminalConfig(UUID.randomUUID(), request.c2bFieldName(), request.b2cFieldName(),
                    request.tkbPayPrefix(), "ACTIVE", 2);
        }

        @Override
        public List<TkbPayEntry> listTkbPay() {
            return List.of();
        }

        @Override
        public TkbPayEntry addTkbPay(TkbPayEntryRequest request) {
            return new TkbPayEntry(UUID.randomUUID(), request.rcvTspId(), "DRAFT", false);
        }

        @Override
        public TkbPayEntry removeTkbPay(UUID id) {
            return new TkbPayEntry(id, "100000", "REMOVED", true);
        }

        @Override
        public List<RoutingFlag> listRoutingFlags() {
            return List.of();
        }

        @Override
        public RoutingFlag setRoutingFlag(String key, RoutingFlagRequest request) {
            return new RoutingFlag(UUID.randomUUID(), key, request.value(), "ACTIVE");
        }

        @Override
        public PendingChanges pendingChanges() {
            return new PendingChanges(0, 1, 2, List.of());
        }

        @Override
        public void discardDrafts() {
            // no-op
        }

        @Override
        public RoutingManifest publishManifest() {
            if (publishShouldThrowConflict) {
                ProblemEnvelope envelope = new ProblemEnvelope(
                        new ProblemDetail(
                                "https://contracts.newpay/errors/routing-manifest-conflict",
                                "Routing manifest conflict",
                                409,
                                "ROUTING_MANIFEST_CONFLICT",
                                "x",
                                null,
                                "t"),
                        NOW);
                throw new UpstreamProblemException(org.springframework.http.HttpStatus.CONFLICT, envelope);
            }
            return new RoutingManifest(UUID.randomUUID(), 1, "ACTIVE", "sha256:abc", NOW, null, null);
        }

        @Override
        public RoutingManifest latestManifest() {
            return new RoutingManifest(UUID.randomUUID(), 1, "ACTIVE", "sha256:abc", NOW, null, null);
        }

        @Override
        public RoutingManifest getManifest(UUID manifestId) {
            return new RoutingManifest(manifestId, 1, "ACTIVE", "sha256:abc", NOW, null, null);
        }
    }
}
