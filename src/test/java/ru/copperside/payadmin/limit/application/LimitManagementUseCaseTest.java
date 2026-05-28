package ru.copperside.payadmin.limit.application;

import org.junit.jupiter.api.Test;
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
import ru.copperside.payadmin.limit.domain.RuleDictionaries;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LimitManagementUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-05-27T09:00:00Z");

    private final FakePort port = new FakePort();
    private final LimitManagementUseCase useCase = new LimitManagementUseCase(port);

    @Test
    void delegatesGroupTypeListingToPort() {
        List<MerchantGroupType> types = useCase.listGroupTypes();

        assertThat(types).hasSize(1);
        assertThat(types.getFirst().code()).isEqualTo("risk-tier");
    }

    @Test
    void delegatesGroupListingToPort() {
        List<MerchantGroup> groups = useCase.listGroups(port.typeId);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().code()).isEqualTo("risk-high");
        assertThat(port.capturedTypeId).isEqualTo(port.typeId);
    }

    @Test
    void delegatesMembershipListingToPort() {
        MembershipQuery query = new MembershipQuery("502118", port.typeId, port.groupId, "all");

        List<MerchantGroupMembership> memberships = useCase.listMemberships(query);

        assertThat(memberships).hasSize(1);
        assertThat(memberships.getFirst().merchantId()).isEqualTo("502118");
        assertThat(port.capturedQuery).isEqualTo(query);
    }

    @Test
    void delegatesGroupTypeCreationToPort() {
        CreateGroupTypeCommand command = new CreateGroupTypeCommand("risk-tier", "Risk tier", null, 10);

        MerchantGroupType type = useCase.createGroupType(command);

        assertThat(type.code()).isEqualTo("risk-tier");
        assertThat(port.capturedCreateGroupTypeCommand).isEqualTo(command);
    }

    @Test
    void delegatesGroupTypePatchToPort() {
        PatchGroupTypeCommand command = new PatchGroupTypeCommand("Risk tier", "Updated", false, 20);

        MerchantGroupType type = useCase.patchGroupType(port.typeId, command);

        assertThat(type.enabled()).isFalse();
        assertThat(port.capturedGroupTypeId).isEqualTo(port.typeId);
        assertThat(port.capturedPatchGroupTypeCommand).isEqualTo(command);
    }

    @Test
    void delegatesGroupCreationToPort() {
        CreateGroupCommand command = new CreateGroupCommand(port.typeId, "risk-high", "High risk", null);

        MerchantGroup group = useCase.createGroup(command);

        assertThat(group.code()).isEqualTo("risk-high");
        assertThat(port.capturedCreateGroupCommand).isEqualTo(command);
    }

    @Test
    void delegatesGroupPatchToPort() {
        PatchGroupCommand command = new PatchGroupCommand("High risk updated", "Updated", false);

        MerchantGroup group = useCase.patchGroup(port.groupId, command);

        assertThat(group.enabled()).isFalse();
        assertThat(port.capturedGroupId).isEqualTo(port.groupId);
        assertThat(port.capturedPatchGroupCommand).isEqualTo(command);
    }

    @Test
    void delegatesMembershipAssignmentToPort() {
        AssignMembershipCommand command = new AssignMembershipCommand("502118", port.groupId, NOW);

        MerchantGroupMembership membership = useCase.assignMembership(command);

        assertThat(membership.merchantId()).isEqualTo("502118");
        assertThat(port.capturedAssignMembershipCommand).isEqualTo(command);
    }

    @Test
    void delegatesMembershipCloseToPort() {
        CloseMembershipCommand command = new CloseMembershipCommand(Instant.parse("2026-05-27T15:00:00Z"));

        MerchantGroupMembership membership = useCase.closeMembership(port.membershipId, command);

        assertThat(membership.validTo()).isEqualTo(command.validTo());
        assertThat(port.capturedMembershipId).isEqualTo(port.membershipId);
        assertThat(port.capturedCloseMembershipCommand).isEqualTo(command);
    }

    @Test
    void delegatesOperationTypeListingToPort() {
        List<OperationType> types = useCase.listOperationTypes();

        assertThat(types).hasSize(1);
        assertThat(types.getFirst().code()).isEqualTo("SBP_C2B");
    }

    @Test
    void delegatesRuleDictionariesToPort() {
        RuleDictionaries dictionaries = useCase.getRuleDictionaries();

        assertThat(dictionaries.operationFamilies()).extracting(DictionaryItem::code).contains("SBP");
    }

    @Test
    void delegatesOperationTypeCreationToPort() {
        CreateOperationTypeCommand command = new CreateOperationTypeCommand(
                "SBP_C2C",
                "SBP C2C",
                "SBP",
                OperationDirection.ALL
        );

        OperationType type = useCase.createOperationType(command);

        assertThat(type.code()).isEqualTo("SBP_C2C");
        assertThat(port.capturedCreateOperationTypeCommand).isEqualTo(command);
    }

    @Test
    void delegatesOperationTypePatchToPort() {
        PatchOperationTypeCommand command = new PatchOperationTypeCommand(
                "SBP C2B updated",
                "SBP",
                OperationDirection.IN,
                false
        );

        OperationType type = useCase.patchOperationType(port.operationTypeId, command);

        assertThat(type.enabled()).isFalse();
        assertThat(port.capturedOperationTypeId).isEqualTo(port.operationTypeId);
        assertThat(port.capturedPatchOperationTypeCommand).isEqualTo(command);
    }

    @Test
    void delegatesRuleListingToPort() {
        List<LimitRule> rules = useCase.listRules();

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().operationSelector().value()).isEqualTo("SBP_C2B");
    }

    @Test
    void delegatesRuleReadToPort() {
        LimitRule rule = useCase.getRule(port.ruleId);

        assertThat(rule.id()).isEqualTo(port.ruleId);
        assertThat(port.capturedGetRuleId).isEqualTo(port.ruleId);
    }

    @Test
    void delegatesRuleCreationToPort() {
        CreateLimitRuleCommand command = new CreateLimitRuleCommand(
                "RULE_SBP_C2B_DAY",
                "SBP C2B daily amount",
                new LimitRuleSelector("TYPE", "SBP_C2B"),
                OperationDirection.IN,
                new LimitRuleSelector("NONE", null),
                LimitTargetType.PHONE,
                LimitRuleMetric.AMOUNT,
                LimitRulePeriod.DAY,
                "RUB"
        );

        LimitRule rule = useCase.createRule(command);

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.DRAFT);
        assertThat(port.capturedCreateLimitRuleCommand).isEqualTo(command);
    }

    @Test
    void delegatesRulePatchToPort() {
        PatchLimitRuleCommand command = new PatchLimitRuleCommand(
                "SBP C2B weekly count",
                new LimitRuleSelector("TYPE", "SBP_C2B"),
                OperationDirection.IN,
                new LimitRuleSelector("NONE", null),
                LimitTargetType.PHONE,
                LimitRuleMetric.COUNT,
                LimitRulePeriod.WEEK,
                null
        );

        LimitRule rule = useCase.patchRule(port.ruleId, command);

        assertThat(rule.metric()).isEqualTo(LimitRuleMetric.COUNT);
        assertThat(port.capturedRuleId).isEqualTo(port.ruleId);
        assertThat(port.capturedPatchLimitRuleCommand).isEqualTo(command);
    }

    @Test
    void delegatesRuleActivationToPort() {
        LimitRule rule = useCase.activateRule(port.ruleId);

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.ACTIVE);
        assertThat(port.capturedActivateRuleId).isEqualTo(port.ruleId);
    }

    @Test
    void delegatesRuleDisableToPort() {
        LimitRule rule = useCase.disableRule(port.ruleId);

        assertThat(rule.status()).isEqualTo(LimitRuleStatus.DISABLED);
        assertThat(port.capturedDisableRuleId).isEqualTo(port.ruleId);
    }

    @Test
    void delegatesRuleNewVersionToPort() {
        LimitRule rule = useCase.createNewRuleVersion(port.ruleId);

        assertThat(rule.version()).isEqualTo(2);
        assertThat(rule.status()).isEqualTo(LimitRuleStatus.DRAFT);
        assertThat(port.capturedNewVersionRuleId).isEqualTo(port.ruleId);
    }

    static class FakePort implements LimitManagementPort {
        final UUID typeId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final UUID membershipId = UUID.randomUUID();
        final UUID operationTypeId = UUID.randomUUID();
        final UUID ruleId = UUID.randomUUID();
        UUID capturedTypeId;
        UUID capturedGroupTypeId;
        UUID capturedGroupId;
        UUID capturedMembershipId;
        UUID capturedOperationTypeId;
        UUID capturedGetRuleId;
        UUID capturedRuleId;
        UUID capturedActivateRuleId;
        UUID capturedDisableRuleId;
        UUID capturedNewVersionRuleId;
        MembershipQuery capturedQuery;
        CreateGroupTypeCommand capturedCreateGroupTypeCommand;
        PatchGroupTypeCommand capturedPatchGroupTypeCommand;
        CreateGroupCommand capturedCreateGroupCommand;
        PatchGroupCommand capturedPatchGroupCommand;
        AssignMembershipCommand capturedAssignMembershipCommand;
        CloseMembershipCommand capturedCloseMembershipCommand;
        CreateOperationTypeCommand capturedCreateOperationTypeCommand;
        PatchOperationTypeCommand capturedPatchOperationTypeCommand;
        CreateLimitRuleCommand capturedCreateLimitRuleCommand;
        PatchLimitRuleCommand capturedPatchLimitRuleCommand;

        @Override
        public List<MerchantGroupType> listGroupTypes() {
            return List.of(new MerchantGroupType(typeId, "risk-tier", "Risk tier", null, true, 10, NOW, NOW));
        }

        @Override
        public List<MerchantGroup> listGroups(UUID typeId) {
            capturedTypeId = typeId;
            return List.of(new MerchantGroup(groupId, this.typeId, "risk-high", "High risk", null, true, NOW, NOW));
        }

        @Override
        public List<MerchantGroupMembership> listMemberships(MembershipQuery query) {
            capturedQuery = query;
            return List.of(new MerchantGroupMembership(membershipId, "502118", groupId, typeId, NOW, null, NOW, "alice", null, null));
        }

        @Override
        public MerchantGroupType createGroupType(CreateGroupTypeCommand command) {
            capturedCreateGroupTypeCommand = command;
            return new MerchantGroupType(typeId, command.code(), command.name(), command.description(), true, command.sortOrder(), NOW, NOW);
        }

        @Override
        public MerchantGroupType patchGroupType(UUID id, PatchGroupTypeCommand command) {
            capturedGroupTypeId = id;
            capturedPatchGroupTypeCommand = command;
            return new MerchantGroupType(id, "risk-tier", command.name(), command.description(), command.enabled(), command.sortOrder(), NOW, NOW);
        }

        @Override
        public MerchantGroup createGroup(CreateGroupCommand command) {
            capturedCreateGroupCommand = command;
            return new MerchantGroup(groupId, command.typeId(), command.code(), command.name(), command.description(), true, NOW, NOW);
        }

        @Override
        public MerchantGroup patchGroup(UUID id, PatchGroupCommand command) {
            capturedGroupId = id;
            capturedPatchGroupCommand = command;
            return new MerchantGroup(id, typeId, "risk-high", command.name(), command.description(), command.enabled(), NOW, NOW);
        }

        @Override
        public MerchantGroupMembership assignMembership(AssignMembershipCommand command) {
            capturedAssignMembershipCommand = command;
            return new MerchantGroupMembership(membershipId, command.merchantId(), command.groupId(), typeId, command.validFrom(), null, NOW, "alice", null, null);
        }

        @Override
        public MerchantGroupMembership closeMembership(UUID id, CloseMembershipCommand command) {
            capturedMembershipId = id;
            capturedCloseMembershipCommand = command;
            return new MerchantGroupMembership(id, "502118", groupId, typeId, NOW, command.validTo(), NOW, "alice", command.validTo(), "bob");
        }

        @Override
        public List<OperationType> listOperationTypes() {
            return List.of(operationType("SBP_C2B", OperationDirection.IN, true));
        }

        @Override
        public RuleDictionaries getRuleDictionaries() {
            return new RuleDictionaries(
                    List.of(new DictionaryItem("SBP", "SBP", true, 10, NOW, NOW)),
                    listOperationTypes(),
                    List.of(new DictionaryItem("MIR", "MIR", true, 10, NOW, NOW)),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
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
            capturedCreateOperationTypeCommand = command;
            return new OperationType(operationTypeId, command.code(), command.name(), command.familyCode(), command.direction(), true, 10, NOW, NOW);
        }

        @Override
        public OperationType patchOperationType(UUID id, PatchOperationTypeCommand command) {
            capturedOperationTypeId = id;
            capturedPatchOperationTypeCommand = command;
            return new OperationType(id, "SBP_C2B", command.name(), command.familyCode(), command.direction(), command.enabled(), 10, NOW, NOW);
        }

        @Override
        public List<LimitRule> listRules() {
            return List.of(rule(ruleId, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT));
        }

        @Override
        public LimitRule getRule(UUID id) {
            capturedGetRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule createRule(CreateLimitRuleCommand command) {
            capturedCreateLimitRuleCommand = command;
            return rule(ruleId, 1, command.metric(), command.period(), LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule patchRule(UUID id, PatchLimitRuleCommand command) {
            capturedRuleId = id;
            capturedPatchLimitRuleCommand = command;
            return rule(id, 1, command.metric(), command.period(), LimitRuleStatus.DRAFT);
        }

        @Override
        public LimitRule activateRule(UUID id) {
            capturedActivateRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.ACTIVE);
        }

        @Override
        public LimitRule disableRule(UUID id) {
            capturedDisableRuleId = id;
            return rule(id, 1, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DISABLED);
        }

        @Override
        public LimitRule createNewRuleVersion(UUID id) {
            capturedNewVersionRuleId = id;
            return rule(UUID.randomUUID(), 2, LimitRuleMetric.AMOUNT, LimitRulePeriod.DAY, LimitRuleStatus.DRAFT);
        }

        private OperationType operationType(String code, OperationDirection direction, boolean enabled) {
            return new OperationType(operationTypeId, code, code.replace('_', ' '), "SBP", direction, enabled, 10, NOW, NOW);
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
