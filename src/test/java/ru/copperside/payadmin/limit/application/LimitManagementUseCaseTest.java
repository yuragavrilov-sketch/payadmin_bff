package ru.copperside.payadmin.limit.application;

import org.junit.jupiter.api.Test;
import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;

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

    static class FakePort implements LimitManagementPort {
        final UUID typeId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final UUID membershipId = UUID.randomUUID();
        UUID capturedTypeId;
        UUID capturedGroupTypeId;
        UUID capturedGroupId;
        UUID capturedMembershipId;
        MembershipQuery capturedQuery;
        CreateGroupTypeCommand capturedCreateGroupTypeCommand;
        PatchGroupTypeCommand capturedPatchGroupTypeCommand;
        CreateGroupCommand capturedCreateGroupCommand;
        PatchGroupCommand capturedPatchGroupCommand;
        AssignMembershipCommand capturedAssignMembershipCommand;
        CloseMembershipCommand capturedCloseMembershipCommand;

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
    }
}
