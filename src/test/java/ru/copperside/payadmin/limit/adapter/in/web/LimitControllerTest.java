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
import ru.copperside.payadmin.limit.application.CreateLimitRuleCommand;
import ru.copperside.payadmin.limit.application.CreateOperationTypeCommand;
import ru.copperside.payadmin.limit.application.MembershipQuery;
import ru.copperside.payadmin.limit.application.PatchGroupCommand;
import ru.copperside.payadmin.limit.application.PatchGroupTypeCommand;
import ru.copperside.payadmin.limit.application.PatchLimitRuleCommand;
import ru.copperside.payadmin.limit.application.PatchOperationTypeCommand;
import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;
import ru.copperside.payadmin.limit.domain.DictionaryItem;
import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;
import ru.copperside.payadmin.limit.domain.LimitRuleSelector;
import ru.copperside.payadmin.limit.domain.LimitRuleStatus;
import ru.copperside.payadmin.limit.domain.LimitTargetType;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;
import ru.copperside.payadmin.limit.domain.OperationDirection;
import ru.copperside.payadmin.limit.domain.OperationType;
import ru.copperside.payadmin.limit.domain.RuleManifest;
import ru.copperside.payadmin.limit.domain.RuleDictionaries;

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
    private static final UUID OPERATION_TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MANIFEST_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
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

    @Test
    void listsOperationTypes() throws Exception {
        mockMvc.perform(get("/api/v1/limits/operation-types")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("SBP_C2B"))
                .andExpect(jsonPath("$.data[0].familyCode").value("SBP"))
                .andExpect(jsonPath("$.data[0].direction").value("IN"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-27T09:00:00Z"));
    }

    @Test
    void createsOperationType() throws Exception {
        mockMvc.perform(post("/api/v1/limits/operation-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SBP_C2C",
                                  "name": "SBP C2C",
                                  "familyCode": "SBP",
                                  "direction": "ALL"
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SBP_C2C"));

        assertThat(limitManagementPort.lastCreateOperationTypeCommand)
                .isEqualTo(new CreateOperationTypeCommand("SBP_C2C", "SBP C2C", "SBP", OperationDirection.ALL));
    }

    @Test
    void patchesOperationType() throws Exception {
        mockMvc.perform(patch("/api/v1/limits/operation-types/{typeId}", OPERATION_TYPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "SBP C2B updated",
                                  "familyCode": "SBP",
                                  "direction": "IN",
                                  "enabled": false
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(limitManagementPort.lastPatchOperationTypeId).isEqualTo(OPERATION_TYPE_ID);
        assertThat(limitManagementPort.lastPatchOperationTypeCommand)
                .isEqualTo(new PatchOperationTypeCommand("SBP C2B updated", "SBP", OperationDirection.IN, false));
    }

    @Test
    void listsRules() throws Exception {
        mockMvc.perform(get("/api/v1/limits/rules")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].direction").value("IN"))
                .andExpect(jsonPath("$.data[0].targetType").value("PHONE"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].operationSelector.value").value("SBP_C2B"));
    }

    @Test
    void returnsRuleDictionaries() throws Exception {
        mockMvc.perform(get("/api/v1/limits/rule-dictionaries")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationFamilies[0].code").value("SBP"))
                .andExpect(jsonPath("$.data.operationTypes[0].code").value("SBP_C2B"))
                .andExpect(jsonPath("$.data.operationSelectorTypes[0]").value("ANY"));
    }

    @Test
    void getsRule() throws Exception {
        mockMvc.perform(get("/api/v1/limits/rules/{ruleId}", RULE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(RULE_ID.toString()))
                .andExpect(jsonPath("$.data.code").value("RULE_SBP_C2B_DAY"));

        assertThat(limitManagementPort.lastGetLimitRuleId).isEqualTo(RULE_ID);
    }

    @Test
    void createsRule() throws Exception {
        mockMvc.perform(post("/api/v1/limits/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "RULE_SBP_C2B_DAY",
                                  "name": "SBP C2B daily amount",
                                  "operationSelector": { "type": "TYPE", "value": "SBP_C2B" },
                                  "direction": "IN",
                                  "attributeSelector": { "type": "NONE", "value": null },
                                  "targetType": "PHONE",
                                  "metric": "AMOUNT",
                                  "period": "DAY",
                                  "currency": "RUB"
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("RULE_SBP_C2B_DAY"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        assertThat(limitManagementPort.lastCreateLimitRuleCommand)
                .isEqualTo(new CreateLimitRuleCommand(
                        "RULE_SBP_C2B_DAY",
                        "SBP C2B daily amount",
                        new LimitRuleSelector("TYPE", "SBP_C2B"),
                        OperationDirection.IN,
                        new LimitRuleSelector("NONE", null),
                        LimitTargetType.PHONE,
                        LimitRuleMetric.AMOUNT,
                        LimitRulePeriod.DAY,
                        "RUB"
                ));
    }

    @Test
    void patchesRule() throws Exception {
        mockMvc.perform(patch("/api/v1/limits/rules/{ruleId}", RULE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "SBP C2B weekly count",
                                  "operationSelector": { "type": "TYPE", "value": "SBP_C2B" },
                                  "direction": "IN",
                                  "attributeSelector": { "type": "NONE", "value": null },
                                  "targetType": "PHONE",
                                  "metric": "COUNT",
                                  "period": "WEEK",
                                  "currency": null
                                }
                                """)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metric").value("COUNT"))
                .andExpect(jsonPath("$.data.currency").value(nullValue()));

        assertThat(limitManagementPort.lastPatchLimitRuleId).isEqualTo(RULE_ID);
        assertThat(limitManagementPort.lastPatchLimitRuleCommand)
                .isEqualTo(new PatchLimitRuleCommand(
                        "SBP C2B weekly count",
                        new LimitRuleSelector("TYPE", "SBP_C2B"),
                        OperationDirection.IN,
                        new LimitRuleSelector("NONE", null),
                        LimitTargetType.PHONE,
                        LimitRuleMetric.COUNT,
                        LimitRulePeriod.WEEK,
                        null
                ));
    }

    @Test
    void activatesRule() throws Exception {
        mockMvc.perform(post("/api/v1/limits/rules/{ruleId}/activate", RULE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.activatedAt").value("2026-05-27T09:00:00Z"));

        assertThat(limitManagementPort.lastActivateRuleId).isEqualTo(RULE_ID);
    }

    @Test
    void disablesRule() throws Exception {
        mockMvc.perform(post("/api/v1/limits/rules/{ruleId}/disable", RULE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.disabledAt").value("2026-05-27T09:00:00Z"));

        assertThat(limitManagementPort.lastDisableRuleId).isEqualTo(RULE_ID);
    }

    @Test
    void createsNewRuleVersion() throws Exception {
        mockMvc.perform(post("/api/v1/limits/rules/{ruleId}/new-version", RULE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        assertThat(limitManagementPort.lastNewVersionRuleId).isEqualTo(RULE_ID);
    }

    @Test
    void compilesRuleManifest() throws Exception {
        mockMvc.perform(post("/api/v1/limits/rule-manifests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MANIFEST_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("VALID"))
                .andExpect(jsonPath("$.data.ruleCount").value(1))
                .andExpect(jsonPath("$.data.rules[0].code").value("RULE_SBP_C2B_DAY"))
                .andExpect(jsonPath("$.data.rules[0].measure.currency").value("RUB"));

        assertThat(limitManagementPort.compileManifestCalled).isTrue();
    }

    @Test
    void readsLatestRuleManifest() throws Exception {
        mockMvc.perform(get("/api/v1/limits/rule-manifests/latest")
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MANIFEST_ID.toString()))
                .andExpect(jsonPath("$.data.checksum").value("sha256:test"));
    }

    @Test
    void readsRuleManifestById() throws Exception {
        mockMvc.perform(get("/api/v1/limits/rule-manifests/{manifestId}", MANIFEST_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("payadmin.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(MANIFEST_ID.toString()));

        assertThat(limitManagementPort.lastManifestId).isEqualTo(MANIFEST_ID);
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
        private CreateOperationTypeCommand lastCreateOperationTypeCommand;
        private UUID lastPatchOperationTypeId;
        private PatchOperationTypeCommand lastPatchOperationTypeCommand;
        private UUID lastGetLimitRuleId;
        private CreateLimitRuleCommand lastCreateLimitRuleCommand;
        private UUID lastPatchLimitRuleId;
        private PatchLimitRuleCommand lastPatchLimitRuleCommand;
        private UUID lastActivateRuleId;
        private UUID lastDisableRuleId;
        private UUID lastNewVersionRuleId;
        private UUID lastManifestId;
        private boolean compileManifestCalled;

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
            lastCreateOperationTypeCommand = null;
            lastPatchOperationTypeId = null;
            lastPatchOperationTypeCommand = null;
            lastGetLimitRuleId = null;
            lastCreateLimitRuleCommand = null;
            lastPatchLimitRuleId = null;
            lastPatchLimitRuleCommand = null;
            lastActivateRuleId = null;
            lastDisableRuleId = null;
            lastNewVersionRuleId = null;
            lastManifestId = null;
            compileManifestCalled = false;
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

        @Override
        public List<OperationType> listOperationTypes() {
            return List.of(operationType("SBP_C2B", "SBP C2B", OperationDirection.IN, true));
        }

        @Override
        public RuleDictionaries getRuleDictionaries() {
            return new RuleDictionaries(
                    List.of(dictionaryItem("SBP")),
                    listOperationTypes(),
                    List.of(dictionaryItem("MIR")),
                    List.of(dictionaryItem("RU")),
                    List.of(dictionaryItem("TKB")),
                    List.of(dictionaryItem("220220")),
                    List.of(dictionaryItem("DEBIT")),
                    List.of(dictionaryItem("STANDARD")),
                    List.of("IN", "OUT", "ALL"),
                    List.of("ANY", "FAMILY", "TYPE"),
                    List.of("NONE", "PAYMENT_SYSTEM"),
                    List.of("ANY", "CARD", "PHONE"),
                    List.of("AMOUNT", "COUNT"),
                    List.of("DAY", "WEEK", "MONTH")
            );
        }

        @Override
        public OperationType createOperationType(CreateOperationTypeCommand command) {
            lastCreateOperationTypeCommand = command;
            return new OperationType(OPERATION_TYPE_ID, command.code(), command.name(), command.familyCode(), command.direction(), true, 10, NOW, NOW);
        }

        @Override
        public OperationType patchOperationType(UUID id, PatchOperationTypeCommand command) {
            lastPatchOperationTypeId = id;
            lastPatchOperationTypeCommand = command;
            return new OperationType(id, "SBP_C2B", command.name(), command.familyCode(), command.direction(), command.enabled(), 10, NOW, NOW);
        }

        @Override
        public List<LimitRule> listRules() {
            return List.of(rule(RULE_ID, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT));
        }

        @Override
        public LimitRule getRule(UUID id) {
            lastGetLimitRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule createRule(CreateLimitRuleCommand command) {
            lastCreateLimitRuleCommand = command;
            return rule(RULE_ID, 1, command.metric(), command.period(), LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule patchRule(UUID id, PatchLimitRuleCommand command) {
            lastPatchLimitRuleId = id;
            lastPatchLimitRuleCommand = command;
            return rule(id, 1, command.metric(), command.period(), LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule activateRule(UUID id) {
            lastActivateRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.ACTIVE);
        }

        @Override
        public LimitRule disableRule(UUID id) {
            lastDisableRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DISABLED);
        }

        @Override
        public LimitRule createNewRuleVersion(UUID id) {
            lastNewVersionRuleId = id;
            return rule(UUID.fromString("44444444-4444-4444-4444-444444444444"), 2, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT);
        }

        @Override
        public RuleManifest compileRuleManifest() {
            compileManifestCalled = true;
            return manifest();
        }

        @Override
        public RuleManifest getLatestRuleManifest() {
            return manifest();
        }

        @Override
        public RuleManifest getRuleManifest(UUID id) {
            lastManifestId = id;
            return manifest();
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

        private OperationType operationType(String code, String name, OperationDirection direction, boolean enabled) {
            return new OperationType(OPERATION_TYPE_ID, code, name, "SBP", direction, enabled, 10, NOW, NOW);
        }

        private DictionaryItem dictionaryItem(String code) {
            return new DictionaryItem(code, code, true, 10, NOW, NOW);
        }

        private RuleManifest manifest() {
            return new RuleManifest(
                    MANIFEST_ID,
                    1,
                    "VALID",
                    "sha256:test",
                    1,
                    NOW,
                    List.of(new RuleManifest.CompiledRule(
                            RULE_ID,
                            "RULE_SBP_C2B_DAY",
                            1,
                            new RuleManifest.Matcher(
                                    new LimitRuleSelector("TYPE", "SBP_C2B"),
                                    OperationDirection.IN,
                                    new LimitRuleSelector("NONE", null),
                                    LimitTargetType.PHONE
                            ),
                            new RuleManifest.Measure(LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, "RUB")
                    )),
                    List.of()
            );
        }

        private LimitRule rule(
                UUID id,
                int version,
                LimitRuleMetric metric,
                LimitRulePeriod period,
                LimitRuleStatus status
        ) {
            Instant activatedAt = status == LimitRuleStatus.ACTIVE || status == LimitRuleStatus.DISABLED ? NOW : null;
            Instant disabledAt = status == LimitRuleStatus.DISABLED ? NOW : null;
            return new LimitRule(
                    id,
                    "RULE_SBP_C2B_DAY",
                    version,
                    "SBP C2B daily amount",
                    OperationDirection.IN,
                    new LimitRuleSelector("TYPE", "SBP_C2B"),
                    new LimitRuleSelector("NONE", null),
                    LimitTargetType.PHONE,
                    metric,
                    period,
                    metric == LimitRuleMetric.AMOUNT ? "RUB" : null,
                    null,
                    null,
                    status,
                    NOW,
                    NOW,
                    activatedAt,
                    disabledAt,
                    status == LimitRuleStatus.ACTIVE
            );
        }
    }
}
