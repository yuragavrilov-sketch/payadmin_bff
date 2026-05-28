package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import ru.copperside.payadmin.common.application.UpstreamProblemException;
import ru.copperside.payadmin.common.web.RequestIdFilter;
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
import ru.copperside.payadmin.limit.config.LimitManagementProperties;
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
import ru.copperside.payadmin.limit.domain.RuleDictionaries;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpLimitManagementAdapterTest {

    private static final UUID TYPE_ID = UUID.fromString("77fe773f-f6f3-4e07-b6fb-d707f7e373d3");
    private static final UUID GROUP_ID = UUID.fromString("1cf49b4d-c96c-4ff4-8f85-5cb508fb8137");
    private static final UUID MEMBERSHIP_ID = UUID.fromString("e0395e16-34cc-4cc3-87c0-8d9cc6d17bd2");
    private static final UUID OPERATION_TYPE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-05-27T09:00:00Z");

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        MDC.put(RequestIdFilter.MDC_KEY, "00000000-0000-0000-0000-000000000111");
    }

    @AfterEach
    void tearDown() throws Exception {
        MDC.clear();
        server.shutdown();
    }

    @Test
    void listGroupTypesSendsInternalHeadersAndMapsEnvelope() throws Exception {
        server.enqueue(json(groupTypesEnvelope()));

        List<MerchantGroupType> types = client().listGroupTypes();

        assertThat(types).containsExactly(new MerchantGroupType(
                TYPE_ID, "risk-tier", "Risk tier", null, true, 10, NOW, NOW));
        assertRequest("/internal/v1/limit-management/merchant-group-types");
    }

    @Test
    void listGroupsAddsTypeIdWhenPresent() throws Exception {
        server.enqueue(json(groupsEnvelope()));

        List<MerchantGroup> groups = client().listGroups(TYPE_ID);

        assertThat(groups).containsExactly(new MerchantGroup(
                GROUP_ID, TYPE_ID, "risk-high", "High risk", "High risk merchants", true, NOW, NOW));
        assertRequest("/internal/v1/limit-management/merchant-groups?typeId=" + TYPE_ID);
    }

    @Test
    void listMembershipsSendsFilters() throws Exception {
        server.enqueue(json(membershipsEnvelope(null)));

        List<MerchantGroupMembership> memberships = client().listMemberships(new MembershipQuery("502118", null, null, "all"));

        assertThat(memberships).hasSize(1);
        assertThat(memberships.getFirst().merchantId()).isEqualTo("502118");
        assertRequest("/internal/v1/limit-management/merchant-group-memberships?merchantId=502118&state=all");
    }

    @Test
    void createGroupTypePostsJson() throws Exception {
        server.enqueue(json(groupTypeEnvelope(true)));

        MerchantGroupType type = client().createGroupType(new CreateGroupTypeCommand("risk-tier", "Risk tier", null, 10));

        assertThat(type.code()).isEqualTo("risk-tier");
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-group-types");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8())
                .contains("\"code\":\"risk-tier\"")
                .contains("\"name\":\"Risk tier\"")
                .contains("\"sortOrder\":10");
    }

    @Test
    void patchGroupTypeSendsPatch() throws Exception {
        server.enqueue(json(groupTypeEnvelope(false)));

        MerchantGroupType type = client().patchGroupType(TYPE_ID, new PatchGroupTypeCommand("Risk tier", "Updated", false, 20));

        assertThat(type.enabled()).isFalse();
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-group-types/" + TYPE_ID);
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getBody().readUtf8()).contains("\"enabled\":false");
    }

    @Test
    void createGroupPostsJson() throws Exception {
        server.enqueue(json(groupEnvelope(true)));

        MerchantGroup group = client().createGroup(new CreateGroupCommand(TYPE_ID, "risk-high", "High risk", "High risk merchants"));

        assertThat(group.code()).isEqualTo("risk-high");
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-groups");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8())
                .contains("\"typeId\":\"" + TYPE_ID + "\"")
                .contains("\"code\":\"risk-high\"");
    }

    @Test
    void patchGroupSendsPatch() throws Exception {
        server.enqueue(json(groupEnvelope(false)));

        MerchantGroup group = client().patchGroup(GROUP_ID, new PatchGroupCommand("High risk", "Updated", false));

        assertThat(group.enabled()).isFalse();
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-groups/" + GROUP_ID);
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getBody().readUtf8()).contains("\"enabled\":false");
    }

    @Test
    void assignMembershipPostsJson() throws Exception {
        server.enqueue(json(membershipEnvelope(null)));

        MerchantGroupMembership membership = client().assignMembership(new AssignMembershipCommand("502118", GROUP_ID, NOW));

        assertThat(membership.merchantId()).isEqualTo("502118");
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-group-memberships");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8())
                .contains("\"merchantId\":\"502118\"")
                .contains("\"groupId\":\"" + GROUP_ID + "\"")
                .contains("\"validFrom\":\"2026-05-27T09:00:00Z\"");
    }

    @Test
    void closeMembershipPostsJson() throws Exception {
        Instant validTo = Instant.parse("2026-05-27T15:00:00Z");
        server.enqueue(json(membershipEnvelope(validTo)));

        MerchantGroupMembership membership = client().closeMembership(MEMBERSHIP_ID, new CloseMembershipCommand(validTo));

        assertThat(membership.validTo()).isEqualTo(validTo);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/merchant-group-memberships/" + MEMBERSHIP_ID + "/close");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).contains("\"validTo\":\"2026-05-27T15:00:00Z\"");
    }

    @Test
    void listOperationTypesSendsInternalHeadersAndMapsEnvelope() throws Exception {
        server.enqueue(json(operationTypesEnvelope()));

        List<OperationType> types = client().listOperationTypes();

        assertThat(types).containsExactly(new OperationType(
                OPERATION_TYPE_ID, "SBP_C2B", "SBP C2B", "SBP", OperationDirection.IN, true, 10, NOW, NOW));
        assertRequest("/internal/v1/limit-management/operation-types");
    }

    @Test
    void getRuleDictionariesMapsEnvelope() throws Exception {
        server.enqueue(json(ruleDictionariesEnvelope()));

        RuleDictionaries dictionaries = client().getRuleDictionaries();

        assertThat(dictionaries.operationFamilies().getFirst().code()).isEqualTo("SBP");
        assertThat(dictionaries.operationSelectorTypes()).contains("ANY", "FAMILY", "TYPE");
        assertRequest("/internal/v1/limit-management/rule-dictionaries");
    }

    @Test
    void createOperationTypePostsJson() throws Exception {
        server.enqueue(json(operationTypeEnvelope(true)));

        OperationType type = client().createOperationType(new CreateOperationTypeCommand("SBP_C2C", "SBP C2C", "SBP", OperationDirection.ALL));

        assertThat(type.direction()).isEqualTo(OperationDirection.IN);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/operation-types");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8())
                .contains("\"code\":\"SBP_C2C\"")
                .contains("\"familyCode\":\"SBP\"")
                .contains("\"direction\":\"ALL\"");
    }

    @Test
    void patchOperationTypeSendsPatch() throws Exception {
        server.enqueue(json(operationTypeEnvelope(false)));

        OperationType type = client().patchOperationType(OPERATION_TYPE_ID, new PatchOperationTypeCommand(
                "SBP C2B updated",
                "SBP",
                OperationDirection.IN,
                false
        ));

        assertThat(type.enabled()).isFalse();
        RecordedRequest request = assertRequest("/internal/v1/limit-management/operation-types/" + OPERATION_TYPE_ID);
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getBody().readUtf8())
                .contains("\"name\":\"SBP C2B updated\"")
                .contains("\"enabled\":false");
    }

    @Test
    void listRulesMapsVersionedRuleFields() throws Exception {
        server.enqueue(json(rulesEnvelope(LimitRuleStatus.DRAFT, 1)));

        List<LimitRule> rules = client().listRules();

        assertThat(rules).hasSize(1);
        LimitRule rule = rules.getFirst();
        assertThat(rule.operationSelector()).isEqualTo(new LimitRuleSelector("TYPE", "SBP_C2B"));
        assertThat(rule.direction()).isEqualTo(OperationDirection.IN);
        assertThat(rule.metric()).isEqualTo(LimitRuleMetric.AMOUNT);
        assertThat(rule.period()).isEqualTo(LimitRulePeriod.DAY);
        assertThat(rule.enabled()).isFalse();
        assertRequest("/internal/v1/limit-management/rules");
    }

    @Test
    void getRuleReadsSingleRule() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.DRAFT, 1)));

        LimitRule rule = client().getRule(RULE_ID);

        assertThat(rule.id()).isEqualTo(RULE_ID);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules/" + RULE_ID);
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    void createRulePostsJson() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.DRAFT, 1)));

        LimitRule rule = client().createRule(new CreateLimitRuleCommand(
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

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.DRAFT);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8())
                .contains("\"code\":\"RULE_SBP_C2B_DAY\"")
                .contains("\"operationSelector\":{\"type\":\"TYPE\",\"value\":\"SBP_C2B\"}")
                .contains("\"direction\":\"IN\"")
                .contains("\"metric\":\"AMOUNT\"")
                .contains("\"period\":\"DAY\"");
    }

    @Test
    void patchRuleSendsPatch() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.DRAFT, 1)));

        LimitRule rule = client().patchRule(RULE_ID, new PatchLimitRuleCommand(
                "SBP C2B weekly count",
                new LimitRuleSelector("TYPE", "SBP_C2B"),
                OperationDirection.IN,
                new LimitRuleSelector("NONE", null),
                LimitTargetType.PHONE,
                LimitRuleMetric.COUNT,
                LimitRulePeriod.WEEK,
                null
        ));

        assertThat(rule.id()).isEqualTo(RULE_ID);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules/" + RULE_ID);
        assertThat(request.getMethod()).isEqualTo("PATCH");
        assertThat(request.getBody().readUtf8())
                .contains("\"name\":\"SBP C2B weekly count\"")
                .contains("\"metric\":\"COUNT\"")
                .contains("\"period\":\"WEEK\"");
    }

    @Test
    void propagatesUpstreamProblemResponses() {
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/problem+json")
                .setBody("""
                        {
                          "error": {
                            "type": "https://contracts.newpay/errors/rule-status-conflict",
                            "title": "Rule status conflict",
                            "status": 409,
                            "code": "RULE_STATUS_CONFLICT",
                            "message": "Only DRAFT rules can be activated",
                            "details": null,
                            "traceId": "00000000-0000-0000-0000-000000000111"
                          },
                          "timestamp": "2026-05-27T09:00:00Z"
                        }
                        """));

        assertThatThrownBy(() -> client().activateRule(RULE_ID))
                .isInstanceOf(UpstreamProblemException.class)
                .satisfies(error -> {
                    UpstreamProblemException problem = (UpstreamProblemException) error;
                    assertThat(problem.statusCode().value()).isEqualTo(409);
                    assertThat(problem.problem().error().code()).isEqualTo("RULE_STATUS_CONFLICT");
                });
    }

    @Test
    void activateRulePostsLifecycleRequest() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.ACTIVE, 1)));

        LimitRule rule = client().activateRule(RULE_ID);

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.ACTIVE);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules/" + RULE_ID + "/activate");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEmpty();
    }

    @Test
    void disableRulePostsLifecycleRequest() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.DISABLED, 1)));

        LimitRule rule = client().disableRule(RULE_ID);

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.DISABLED);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules/" + RULE_ID + "/disable");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEmpty();
    }

    @Test
    void createNewRuleVersionPostsLifecycleRequest() throws Exception {
        server.enqueue(json(ruleEnvelope(LimitRuleStatus.DRAFT, 2)));

        LimitRule rule = client().createNewRuleVersion(RULE_ID);

        assertThat(rule.version()).isEqualTo(2);
        RecordedRequest request = assertRequest("/internal/v1/limit-management/rules/" + RULE_ID + "/new-version");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getBody().readUtf8()).isEmpty();
    }

    private RecordedRequest assertRequest(String path) throws Exception {
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo(path);
        assertThat(request.getHeader("X-Limit-Key")).isEqualTo("secret");
        assertThat(request.getHeader("X-Request-Id")).isEqualTo("00000000-0000-0000-0000-000000000111");
        return request;
    }

    private HttpLimitManagementAdapter client() {
        return new HttpLimitManagementAdapter(new LimitManagementProperties(
                server.url("/").toString(),
                "X-Limit-Key",
                "secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        ));
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private String groupTypesEnvelope() {
        return """
                {
                  "data": [%s],
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(groupTypeJson(true));
    }

    private String groupTypeEnvelope(boolean enabled) {
        return """
                {
                  "data": %s,
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(groupTypeJson(enabled));
    }

    private String groupTypeJson(boolean enabled) {
        return """
                {
                  "id": "%s",
                  "code": "risk-tier",
                  "name": "Risk tier",
                  "description": null,
                  "enabled": %s,
                  "sortOrder": 10,
                  "createdAt": "2026-05-27T09:00:00Z",
                  "updatedAt": "2026-05-27T09:00:00Z"
                }
                """.formatted(TYPE_ID, enabled);
    }

    private String groupsEnvelope() {
        return """
                {
                  "data": [%s],
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(groupJson(true));
    }

    private String groupEnvelope(boolean enabled) {
        return """
                {
                  "data": %s,
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(groupJson(enabled));
    }

    private String groupJson(boolean enabled) {
        return """
                {
                  "id": "%s",
                  "typeId": "%s",
                  "code": "risk-high",
                  "name": "High risk",
                  "description": "High risk merchants",
                  "enabled": %s,
                  "createdAt": "2026-05-27T09:00:00Z",
                  "updatedAt": "2026-05-27T09:00:00Z"
                }
                """.formatted(GROUP_ID, TYPE_ID, enabled);
    }

    private String membershipsEnvelope(Instant validTo) {
        return """
                {
                  "data": [%s],
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(membershipJson(validTo));
    }

    private String membershipEnvelope(Instant validTo) {
        return """
                {
                  "data": %s,
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(membershipJson(validTo));
    }

    private String membershipJson(Instant validTo) {
        String validToJson = validTo == null ? "null" : "\"" + validTo + "\"";
        String closedAtJson = validTo == null ? "null" : "\"" + validTo + "\"";
        String closedByJson = validTo == null ? "null" : "\"bob\"";
        return """
                {
                  "id": "%s",
                  "merchantId": "502118",
                  "groupId": "%s",
                  "groupTypeId": "%s",
                  "validFrom": "2026-05-27T09:00:00Z",
                  "validTo": %s,
                  "createdAt": "2026-05-27T09:00:00Z",
                  "createdBy": "alice",
                  "closedAt": %s,
                  "closedBy": %s
                }
                """.formatted(MEMBERSHIP_ID, GROUP_ID, TYPE_ID, validToJson, closedAtJson, closedByJson);
    }

    private String operationTypesEnvelope() {
        return """
                {
                  "data": [%s],
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(operationTypeJson(true));
    }

    private String operationTypeEnvelope(boolean enabled) {
        return """
                {
                  "data": %s,
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(operationTypeJson(enabled));
    }

    private String operationTypeJson(boolean enabled) {
        return """
                {
                  "id": "%s",
                  "code": "SBP_C2B",
                  "name": "SBP C2B",
                  "familyCode": "SBP",
                  "direction": "IN",
                  "enabled": %s,
                  "sortOrder": 10,
                  "createdAt": "2026-05-27T09:00:00Z",
                  "updatedAt": "2026-05-27T09:00:00Z"
                }
                """.formatted(OPERATION_TYPE_ID, enabled);
    }

    private String ruleDictionariesEnvelope() {
        return """
                {
                  "data": {
                    "operationFamilies": [
                      {
                        "code": "SBP",
                        "name": "SBP",
                        "enabled": true,
                        "sortOrder": 10,
                        "createdAt": "2026-05-27T09:00:00Z",
                        "updatedAt": "2026-05-27T09:00:00Z"
                      }
                    ],
                    "operationTypes": [%s],
                    "paymentSystems": [],
                    "issuerCountries": [],
                    "issuerBanks": [],
                    "bins": [],
                    "cardTypes": [],
                    "cardLevels": [],
                    "directions": ["IN", "OUT", "ALL"],
                    "operationSelectorTypes": ["ANY", "FAMILY", "TYPE"],
                    "attributeSelectorTypes": ["NONE", "PAYMENT_SYSTEM"],
                    "targetTypes": ["ANY", "CARD", "PHONE"],
                    "metrics": ["AMOUNT", "COUNT"],
                    "periods": ["DAY", "WEEK", "MONTH"]
                  },
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(operationTypeJson(true));
    }

    private String rulesEnvelope(LimitRuleStatus status, int version) {
        return """
                {
                  "data": [%s],
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(ruleJson(status, version));
    }

    private String ruleEnvelope(LimitRuleStatus status, int version) {
        return """
                {
                  "data": %s,
                  "meta": null,
                  "error": null,
                  "timestamp": "2026-05-27T09:00:00Z"
                }
                """.formatted(ruleJson(status, version));
    }

    private String ruleJson(LimitRuleStatus status, int version) {
        String activatedAtJson = status == LimitRuleStatus.ACTIVE || status == LimitRuleStatus.DISABLED
                ? "\"2026-05-27T09:00:00Z\""
                : "null";
        String disabledAtJson = status == LimitRuleStatus.DISABLED ? "\"2026-05-27T09:00:00Z\"" : "null";
        boolean enabled = status == LimitRuleStatus.ACTIVE;
        return """
                {
                  "id": "%s",
                  "code": "RULE_SBP_C2B_DAY",
                  "version": %s,
                  "name": "SBP C2B daily amount",
                  "direction": "IN",
                  "operationSelector": {
                    "type": "TYPE",
                    "value": "SBP_C2B"
                  },
                  "attributeSelector": {
                    "type": "NONE",
                    "value": null
                  },
                  "targetType": "PHONE",
                  "metric": "AMOUNT",
                  "period": "DAY",
                  "currency": "RUB",
                  "status": "%s",
                  "enabled": %s,
                  "createdAt": "2026-05-27T09:00:00Z",
                  "updatedAt": "2026-05-27T09:00:00Z",
                  "activatedAt": %s,
                  "disabledAt": %s
                }
                """.formatted(RULE_ID, version, status.name(), enabled, activatedAtJson, disabledAtJson);
    }
}
