package ru.copperside.payadmin.limit.application.port.out;

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
import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;
import ru.copperside.payadmin.limit.domain.OperationType;

import java.util.List;
import java.util.UUID;

public interface LimitManagementPort {
    List<MerchantGroupType> listGroupTypes();

    List<MerchantGroup> listGroups(UUID typeId);

    List<MerchantGroupMembership> listMemberships(MembershipQuery query);

    MerchantGroupType createGroupType(CreateGroupTypeCommand command);

    MerchantGroupType patchGroupType(UUID id, PatchGroupTypeCommand command);

    MerchantGroup createGroup(CreateGroupCommand command);

    MerchantGroup patchGroup(UUID id, PatchGroupCommand command);

    MerchantGroupMembership assignMembership(AssignMembershipCommand command);

    MerchantGroupMembership closeMembership(UUID id, CloseMembershipCommand command);

    List<OperationType> listOperationTypes();

    OperationType createOperationType(CreateOperationTypeCommand command);

    OperationType patchOperationType(UUID id, PatchOperationTypeCommand command);

    List<LimitRule> listRules();

    LimitRule getRule(UUID id);

    LimitRule createRule(CreateLimitRuleCommand command);

    LimitRule patchRule(UUID id, PatchLimitRuleCommand command);

    LimitRule activateRule(UUID id);

    LimitRule disableRule(UUID id);

    LimitRule createNewRuleVersion(UUID id);
}
