package ru.copperside.payadmin.limit.adapter.in.web;

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
import ru.copperside.payadmin.limit.application.AssignMembershipCommand;
import ru.copperside.payadmin.limit.application.CloseMembershipCommand;
import ru.copperside.payadmin.limit.application.CreateGroupCommand;
import ru.copperside.payadmin.limit.application.CreateGroupTypeCommand;
import ru.copperside.payadmin.limit.application.MembershipQuery;
import ru.copperside.payadmin.limit.application.PatchGroupCommand;
import ru.copperside.payadmin.limit.application.PatchGroupTypeCommand;
import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(LimitControllerTest.TestSupport.class)
@TestPropertySource(properties = "payadmin-bff.security.required-authority=payadmin.read")
class LimitControllerTest {

    private static final UUID TYPE_ID = UUID.fromString("77fe773f-f6f3-4e07-b6fb-d707f7e373d3");
    private static final UUID GROUP_ID = UUID.fromString("dc6b2af5-8a04-4d05-8c99-33914b6cc3ab");
    private static final UUID MEMBERSHIP_ID = UUID.fromString("49d059b8-93d6-4d89-b681-d7ae68dfc2a1");
    private static final Instant NOW = Instant.parse("2026-05-27T09:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FakeLimitManagementPort limitManagementPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        limitManagementPort.clear();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void listGroupTypesReturnsEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/limits/merchant-group-types")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].code").value("risk-tier"))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").value("2026-05-27T09:00:00Z"));
    }

    @Test
    void createsGroupType() throws Exception {
        mockMvc.perform(post("/api/v1/limits/merchant-group-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "risk-tier",
                                  "name": "Risk tier",
                                  "description": "Risk segmentation",
                                  "sortOrder": 10
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("risk-tier"));

        assertThat(limitManagementPort.lastCreateGroupTypeCommand)
                .isEqualTo(new CreateGroupTypeCommand("risk-tier", "Risk tier", "Risk segmentation", 10));
    }

    @Test
    void patchesGroupType() throws Exception {
        mockMvc.perform(patch("/api/v1/limits/merchant-group-types/{typeId}", TYPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Risk tier updated",
                                  "description": "Updated",
                                  "enabled": false,
                                  "sortOrder": 20
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(limitManagementPort.lastPatchGroupTypeId).isEqualTo(TYPE_ID);
        assertThat(limitManagementPort.lastPatchGroupTypeCommand)
                .isEqualTo(new PatchGroupTypeCommand("Risk tier updated", "Updated", false, 20));
    }

    @Test
    void listsGroups() throws Exception {
        mockMvc.perform(get("/api/v1/limits/merchant-groups")
                        .param("typeId", TYPE_ID.toString())
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("risk-high"));

        assertThat(limitManagementPort.lastListGroupsTypeId).isEqualTo(TYPE_ID);
    }

    @Test
    void createsGroup() throws Exception {
        mockMvc.perform(post("/api/v1/limits/merchant-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "typeId": "%s",
                                  "code": "risk-high",
                                  "name": "High risk",
                                  "description": "High risk merchants"
                                }
                                """.formatted(TYPE_ID))
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.typeId").value(TYPE_ID.toString()));

        assertThat(limitManagementPort.lastCreateGroupCommand)
                .isEqualTo(new CreateGroupCommand(TYPE_ID, "risk-high", "High risk", "High risk merchants"));
    }

    @Test
    void patchesGroup() throws Exception {
        mockMvc.perform(patch("/api/v1/limits/merchant-groups/{groupId}", GROUP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "High risk updated",
                                  "description": "Updated group",
                                  "enabled": false
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("High risk updated"));

        assertThat(limitManagementPort.lastPatchGroupId).isEqualTo(GROUP_ID);
        assertThat(limitManagementPort.lastPatchGroupCommand)
                .isEqualTo(new PatchGroupCommand("High risk updated", "Updated group", false));
    }

    @Test
    void listsMemberships() throws Exception {
        mockMvc.perform(get("/api/v1/limits/merchant-group-memberships")
                        .param("merchantId", "502118")
                        .param("state", "all")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].merchantId").value("502118"));

        assertThat(limitManagementPort.lastMembershipQuery)
                .isEqualTo(new MembershipQuery("502118", null, null, "all"));
    }

    @Test
    void assignsMembership() throws Exception {
        mockMvc.perform(post("/api/v1/limits/merchant-group-memberships")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "502118",
                                  "groupId": "%s",
                                  "validFrom": "2026-05-27T10:00:00Z"
                                }
                                """.formatted(GROUP_ID))
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(GROUP_ID.toString()));

        assertThat(limitManagementPort.lastAssignMembershipCommand)
                .isEqualTo(new AssignMembershipCommand("502118", GROUP_ID, Instant.parse("2026-05-27T10:00:00Z")));
    }

    @Test
    void closesMembership() throws Exception {
        mockMvc.perform(post("/api/v1/limits/merchant-group-memberships/{membershipId}/close", MEMBERSHIP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "validTo": "2026-05-27T15:00:00Z"
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validTo").value("2026-05-27T15:00:00Z"));

        assertThat(limitManagementPort.lastCloseMembershipId).isEqualTo(MEMBERSHIP_ID);
        assertThat(limitManagementPort.lastCloseMembershipCommand)
                .isEqualTo(new CloseMembershipCommand(Instant.parse("2026-05-27T15:00:00Z")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> new Jwt(
                    token,
                    Instant.parse("2026-05-27T08:00:00Z"),
                    Instant.parse("2026-05-27T10:00:00Z"),
                    Map.of("alg", "none"),
                    Map.of("sub", "alice")
            );
        }

        @Bean
        @Primary
        FakeLimitManagementPort fakeLimitManagementPort() {
            return new FakeLimitManagementPort();
        }
    }

    static class FakeLimitManagementPort implements LimitManagementPort {

        private UUID lastListGroupsTypeId;
        private MembershipQuery lastMembershipQuery;
        private CreateGroupTypeCommand lastCreateGroupTypeCommand;
        private UUID lastPatchGroupTypeId;
        private PatchGroupTypeCommand lastPatchGroupTypeCommand;
        private CreateGroupCommand lastCreateGroupCommand;
        private UUID lastPatchGroupId;
        private PatchGroupCommand lastPatchGroupCommand;
        private AssignMembershipCommand lastAssignMembershipCommand;
        private UUID lastCloseMembershipId;
        private CloseMembershipCommand lastCloseMembershipCommand;

        FakeLimitManagementPort clear() {
            lastListGroupsTypeId = null;
            lastMembershipQuery = null;
            lastCreateGroupTypeCommand = null;
            lastPatchGroupTypeId = null;
            lastPatchGroupTypeCommand = null;
            lastCreateGroupCommand = null;
            lastPatchGroupId = null;
            lastPatchGroupCommand = null;
            lastAssignMembershipCommand = null;
            lastCloseMembershipId = null;
            lastCloseMembershipCommand = null;
            return this;
        }

        @Override
        public List<MerchantGroupType> listGroupTypes() {
            return List.of(new MerchantGroupType(
                    TYPE_ID,
                    "risk-tier",
                    "Risk tier",
                    "Risk segmentation",
                    true,
                    10,
                    NOW,
                    NOW
            ));
        }

        @Override
        public List<MerchantGroup> listGroups(UUID typeId) {
            lastListGroupsTypeId = typeId;
            return List.of(group("risk-high", "High risk", true));
        }

        @Override
        public List<MerchantGroupMembership> listMemberships(MembershipQuery query) {
            lastMembershipQuery = query;
            return List.of(membership(Instant.parse("2026-05-27T10:00:00Z"), null));
        }

        @Override
        public MerchantGroupType createGroupType(CreateGroupTypeCommand command) {
            lastCreateGroupTypeCommand = command;
            return new MerchantGroupType(TYPE_ID, command.code(), command.name(), command.description(), true, command.sortOrder(), NOW, NOW);
        }

        @Override
        public MerchantGroupType patchGroupType(UUID id, PatchGroupTypeCommand command) {
            lastPatchGroupTypeId = id;
            lastPatchGroupTypeCommand = command;
            return new MerchantGroupType(
                    id,
                    "risk-tier",
                    command.name(),
                    command.description(),
                    command.enabled(),
                    command.sortOrder(),
                    NOW,
                    NOW
            );
        }

        @Override
        public MerchantGroup createGroup(CreateGroupCommand command) {
            lastCreateGroupCommand = command;
            return new MerchantGroup(GROUP_ID, command.typeId(), command.code(), command.name(), command.description(), true, NOW, NOW);
        }

        @Override
        public MerchantGroup patchGroup(UUID id, PatchGroupCommand command) {
            lastPatchGroupId = id;
            lastPatchGroupCommand = command;
            return new MerchantGroup(id, TYPE_ID, "risk-high", command.name(), command.description(), command.enabled(), NOW, NOW);
        }

        @Override
        public MerchantGroupMembership assignMembership(AssignMembershipCommand command) {
            lastAssignMembershipCommand = command;
            return membership(command.validFrom(), null);
        }

        @Override
        public MerchantGroupMembership closeMembership(UUID id, CloseMembershipCommand command) {
            lastCloseMembershipId = id;
            lastCloseMembershipCommand = command;
            return membership(Instant.parse("2026-05-27T10:00:00Z"), command.validTo());
        }

        private MerchantGroup group(String code, String name, boolean enabled) {
            return new MerchantGroup(GROUP_ID, TYPE_ID, code, name, "High risk merchants", enabled, NOW, NOW);
        }

        private MerchantGroupMembership membership(Instant validFrom, Instant validTo) {
            return new MerchantGroupMembership(
                    MEMBERSHIP_ID,
                    "502118",
                    GROUP_ID,
                    TYPE_ID,
                    validFrom,
                    validTo,
                    NOW,
                    "alice",
                    validTo == null ? null : NOW,
                    validTo == null ? null : "alice"
            );
        }
    }
}
