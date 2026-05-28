package ru.copperside.payadmin.limit.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.limit.application.AssignMembershipCommand;
import ru.copperside.payadmin.limit.application.CloseMembershipCommand;
import ru.copperside.payadmin.limit.application.CreateGroupCommand;
import ru.copperside.payadmin.limit.application.CreateGroupTypeCommand;
import ru.copperside.payadmin.limit.application.CreateLimitRuleCommand;
import ru.copperside.payadmin.limit.application.CreateOperationTypeCommand;
import ru.copperside.payadmin.limit.application.LimitManagementUseCase;
import ru.copperside.payadmin.limit.application.MembershipQuery;
import ru.copperside.payadmin.limit.application.PatchGroupCommand;
import ru.copperside.payadmin.limit.application.PatchGroupTypeCommand;
import ru.copperside.payadmin.limit.application.PatchLimitRuleCommand;
import ru.copperside.payadmin.limit.application.PatchOperationTypeCommand;
import ru.copperside.payadmin.limit.domain.LimitRuleSelector;
import ru.copperside.payadmin.limit.domain.LimitRuleMetric;
import ru.copperside.payadmin.limit.domain.LimitRulePeriod;
import ru.copperside.payadmin.limit.domain.LimitTargetType;
import ru.copperside.payadmin.limit.domain.OperationDirection;
import ru.copperside.payadmin.limit.domain.RuleManifest;
import ru.copperside.payadmin.limit.domain.RuleDictionaries;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/limits")
public class LimitController {

    private final LimitManagementUseCase useCase;
    private final Clock clock;

    public LimitController(LimitManagementUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping("/merchant-group-types")
    public ApiResponse<List<MerchantGroupTypeResponse>> listGroupTypes() {
        List<MerchantGroupTypeResponse> data = useCase.listGroupTypes().stream()
                .map(MerchantGroupTypeResponse::from)
                .toList();
        return ApiResponse.success(data, clock);
    }

    @PostMapping("/merchant-group-types")
    public ApiResponse<MerchantGroupTypeResponse> createGroupType(@Valid @RequestBody CreateGroupTypeRequest request) {
        var type = useCase.createGroupType(new CreateGroupTypeCommand(
                request.code(),
                request.name(),
                request.description(),
                request.sortOrder()
        ));
        return ApiResponse.success(MerchantGroupTypeResponse.from(type), clock);
    }

    @PatchMapping("/merchant-group-types/{typeId}")
    public ApiResponse<MerchantGroupTypeResponse> patchGroupType(
            @PathVariable UUID typeId,
            @Valid @RequestBody PatchGroupTypeRequest request
    ) {
        var type = useCase.patchGroupType(typeId, new PatchGroupTypeCommand(
                request.name(),
                request.description(),
                request.enabled(),
                request.sortOrder()
        ));
        return ApiResponse.success(MerchantGroupTypeResponse.from(type), clock);
    }

    @GetMapping("/merchant-groups")
    public ApiResponse<List<MerchantGroupResponse>> listGroups(@RequestParam(required = false) UUID typeId) {
        List<MerchantGroupResponse> data = useCase.listGroups(typeId).stream()
                .map(MerchantGroupResponse::from)
                .toList();
        return ApiResponse.success(data, clock);
    }

    @PostMapping("/merchant-groups")
    public ApiResponse<MerchantGroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        var group = useCase.createGroup(new CreateGroupCommand(
                request.typeId(),
                request.code(),
                request.name(),
                request.description()
        ));
        return ApiResponse.success(MerchantGroupResponse.from(group), clock);
    }

    @PatchMapping("/merchant-groups/{groupId}")
    public ApiResponse<MerchantGroupResponse> patchGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody PatchGroupRequest request
    ) {
        var group = useCase.patchGroup(groupId, new PatchGroupCommand(
                request.name(),
                request.description(),
                request.enabled()
        ));
        return ApiResponse.success(MerchantGroupResponse.from(group), clock);
    }

    @GetMapping("/merchant-group-memberships")
    public ApiResponse<List<MerchantGroupMembershipResponse>> listMemberships(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) UUID typeId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(defaultValue = "current") String state
    ) {
        List<MerchantGroupMembershipResponse> data = useCase.listMemberships(
                        new MembershipQuery(merchantId, typeId, groupId, state))
                .stream()
                .map(MerchantGroupMembershipResponse::from)
                .toList();
        return ApiResponse.success(data, clock);
    }

    @PostMapping("/merchant-group-memberships")
    public ApiResponse<MerchantGroupMembershipResponse> assignMembership(@Valid @RequestBody AssignMembershipRequest request) {
        var membership = useCase.assignMembership(new AssignMembershipCommand(
                request.merchantId(),
                request.groupId(),
                request.validFrom()
        ));
        return ApiResponse.success(MerchantGroupMembershipResponse.from(membership), clock);
    }

    @PostMapping("/merchant-group-memberships/{membershipId}/close")
    public ApiResponse<MerchantGroupMembershipResponse> closeMembership(
            @PathVariable UUID membershipId,
            @Valid @RequestBody CloseMembershipRequest request
    ) {
        var membership = useCase.closeMembership(membershipId, new CloseMembershipCommand(request.validTo()));
        return ApiResponse.success(MerchantGroupMembershipResponse.from(membership), clock);
    }

    @GetMapping("/operation-types")
    public ApiResponse<List<OperationTypeResponse>> listOperationTypes() {
        List<OperationTypeResponse> data = useCase.listOperationTypes().stream()
                .map(OperationTypeResponse::from)
                .toList();
        return ApiResponse.success(data, clock);
    }

    @GetMapping("/rule-dictionaries")
    public ApiResponse<RuleDictionaries> getRuleDictionaries() {
        return ApiResponse.success(useCase.getRuleDictionaries(), clock);
    }

    @PostMapping("/operation-types")
    public ApiResponse<OperationTypeResponse> createOperationType(@Valid @RequestBody CreateOperationTypeRequest request) {
        var type = useCase.createOperationType(new CreateOperationTypeCommand(
                request.code(),
                request.name(),
                request.familyCode(),
                request.direction()
        ));
        return ApiResponse.success(OperationTypeResponse.from(type), clock);
    }

    @PatchMapping("/operation-types/{typeId}")
    public ApiResponse<OperationTypeResponse> patchOperationType(
            @PathVariable UUID typeId,
            @Valid @RequestBody PatchOperationTypeRequest request
    ) {
        var type = useCase.patchOperationType(typeId, new PatchOperationTypeCommand(
                request.name(),
                request.familyCode(),
                request.direction(),
                request.enabled()
        ));
        return ApiResponse.success(OperationTypeResponse.from(type), clock);
    }

    @GetMapping("/rules")
    public ApiResponse<List<LimitRuleResponse>> listRules() {
        List<LimitRuleResponse> data = useCase.listRules().stream()
                .map(LimitRuleResponse::from)
                .toList();
        return ApiResponse.success(data, clock);
    }

    @GetMapping("/rules/{ruleId}")
    public ApiResponse<LimitRuleResponse> getRule(@PathVariable UUID ruleId) {
        return ApiResponse.success(LimitRuleResponse.from(useCase.getRule(ruleId)), clock);
    }

    @PostMapping("/rules")
    public ApiResponse<LimitRuleResponse> createRule(@Valid @RequestBody CreateRuleRequest request) {
        var rule = useCase.createRule(new CreateLimitRuleCommand(
                request.code(),
                request.name(),
                request.operationSelector(),
                request.direction(),
                request.attributeSelector(),
                request.targetType(),
                request.metric(),
                request.period(),
                request.currency()
        ));
        return ApiResponse.success(LimitRuleResponse.from(rule), clock);
    }

    @PatchMapping("/rules/{ruleId}")
    public ApiResponse<LimitRuleResponse> patchRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody PatchRuleRequest request
    ) {
        var rule = useCase.patchRule(ruleId, new PatchLimitRuleCommand(
                request.name(),
                request.operationSelector(),
                request.direction(),
                request.attributeSelector(),
                request.targetType(),
                request.metric(),
                request.period(),
                request.currency()
        ));
        return ApiResponse.success(LimitRuleResponse.from(rule), clock);
    }

    @PostMapping("/rules/{ruleId}/activate")
    public ApiResponse<LimitRuleResponse> activateRule(@PathVariable UUID ruleId) {
        return ApiResponse.success(LimitRuleResponse.from(useCase.activateRule(ruleId)), clock);
    }

    @PostMapping("/rules/{ruleId}/disable")
    public ApiResponse<LimitRuleResponse> disableRule(@PathVariable UUID ruleId) {
        return ApiResponse.success(LimitRuleResponse.from(useCase.disableRule(ruleId)), clock);
    }

    @PostMapping("/rules/{ruleId}/new-version")
    public ApiResponse<LimitRuleResponse> createNewRuleVersion(@PathVariable UUID ruleId) {
        return ApiResponse.success(LimitRuleResponse.from(useCase.createNewRuleVersion(ruleId)), clock);
    }

    @PostMapping("/rule-manifests")
    public ApiResponse<RuleManifest> compileRuleManifest() {
        return ApiResponse.success(useCase.compileRuleManifest(), clock);
    }

    @GetMapping("/rule-manifests/latest")
    public ApiResponse<RuleManifest> getLatestRuleManifest() {
        return ApiResponse.success(useCase.getLatestRuleManifest(), clock);
    }

    @GetMapping("/rule-manifests/{manifestId}")
    public ApiResponse<RuleManifest> getRuleManifest(@PathVariable UUID manifestId) {
        return ApiResponse.success(useCase.getRuleManifest(manifestId), clock);
    }

    public record CreateGroupTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull Integer sortOrder
    ) {
    }

    public record PatchGroupTypeRequest(String name, String description, Boolean enabled, Integer sortOrder) {
    }

    public record CreateGroupRequest(
            @NotNull UUID typeId,
            @NotBlank String code,
            @NotBlank String name,
            String description
    ) {
    }

    public record PatchGroupRequest(String name, String description, Boolean enabled) {
    }

    public record AssignMembershipRequest(
            @NotBlank String merchantId,
            @NotNull UUID groupId,
            @NotNull Instant validFrom
    ) {
    }

    public record CloseMembershipRequest(@NotNull Instant validTo) {
    }

    public record CreateOperationTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String familyCode,
            @NotNull OperationDirection direction
    ) {
    }

    public record PatchOperationTypeRequest(String name, String familyCode, OperationDirection direction, Boolean enabled) {
    }

    public record CreateRuleRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull LimitRuleSelector operationSelector,
            @NotNull OperationDirection direction,
            @NotNull LimitRuleSelector attributeSelector,
            @NotNull LimitTargetType targetType,
            @NotNull LimitRuleMetric metric,
            @NotNull LimitRulePeriod period,
            String currency
    ) {
    }

    public record PatchRuleRequest(
            String name,
            LimitRuleSelector operationSelector,
            OperationDirection direction,
            LimitRuleSelector attributeSelector,
            LimitTargetType targetType,
            LimitRuleMetric metric,
            LimitRulePeriod period,
            String currency
    ) {
    }
}
