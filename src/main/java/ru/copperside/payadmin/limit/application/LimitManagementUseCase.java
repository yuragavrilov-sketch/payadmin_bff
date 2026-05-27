package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;
import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;
import ru.copperside.payadmin.limit.domain.OperationType;

import java.util.List;
import java.util.UUID;

public class LimitManagementUseCase {

    private final LimitManagementPort port;

    public LimitManagementUseCase(LimitManagementPort port) {
        this.port = port;
    }

    public List<MerchantGroupType> listGroupTypes() {
        return port.listGroupTypes();
    }

    public List<MerchantGroup> listGroups(UUID typeId) {
        return port.listGroups(typeId);
    }

    public List<MerchantGroupMembership> listMemberships(MembershipQuery query) {
        return port.listMemberships(query);
    }

    public MerchantGroupType createGroupType(CreateGroupTypeCommand command) {
        return port.createGroupType(command);
    }

    public MerchantGroupType patchGroupType(UUID id, PatchGroupTypeCommand command) {
        return port.patchGroupType(id, command);
    }

    public MerchantGroup createGroup(CreateGroupCommand command) {
        return port.createGroup(command);
    }

    public MerchantGroup patchGroup(UUID id, PatchGroupCommand command) {
        return port.patchGroup(id, command);
    }

    public MerchantGroupMembership assignMembership(AssignMembershipCommand command) {
        return port.assignMembership(command);
    }

    public MerchantGroupMembership closeMembership(UUID id, CloseMembershipCommand command) {
        return port.closeMembership(id, command);
    }

    public List<OperationType> listOperationTypes() {
        return port.listOperationTypes();
    }

    public OperationType createOperationType(CreateOperationTypeCommand command) {
        return port.createOperationType(command);
    }

    public OperationType patchOperationType(UUID id, PatchOperationTypeCommand command) {
        return port.patchOperationType(id, command);
    }

    public List<LimitRule> listRules() {
        return port.listRules();
    }

    public LimitRule createRule(CreateLimitRuleCommand command) {
        return port.createRule(command);
    }

    public LimitRule patchRule(UUID id, PatchLimitRuleCommand command) {
        return port.patchRule(id, command);
    }

    public LimitRule activateRule(UUID id) {
        return port.activateRule(id);
    }

    public LimitRule disableRule(UUID id) {
        return port.disableRule(id);
    }

    public LimitRule createNewRuleVersion(UUID id) {
        return port.createNewRuleVersion(id);
    }
}
