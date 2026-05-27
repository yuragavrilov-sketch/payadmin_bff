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
import ru.copperside.payadmin.limit.application.LimitManagementUseCase;
import ru.copperside.payadmin.limit.application.MembershipQuery;
import ru.copperside.payadmin.limit.application.PatchGroupCommand;
import ru.copperside.payadmin.limit.application.PatchGroupTypeCommand;

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
}
