package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.copperside.payadmin.common.application.UpstreamUnavailableException;
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
import ru.copperside.payadmin.limit.application.port.out.LimitManagementPort;
import ru.copperside.payadmin.limit.config.LimitManagementProperties;
import ru.copperside.payadmin.limit.domain.LimitRule;
import ru.copperside.payadmin.limit.domain.MerchantGroup;
import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;
import ru.copperside.payadmin.limit.domain.MerchantGroupType;
import ru.copperside.payadmin.limit.domain.OperationType;

import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class HttpLimitManagementAdapter implements LimitManagementPort {

    private static final ParameterizedTypeReference<LimitManagementApiResponse<List<LimitManagementGroupType>>> GROUP_TYPES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<LimitManagementGroupType>> GROUP_TYPE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<List<LimitManagementGroup>>> GROUPS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<LimitManagementGroup>> GROUP_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<List<LimitManagementMembership>>> MEMBERSHIPS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<LimitManagementMembership>> MEMBERSHIP_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<List<LimitManagementOperationType>>> OPERATION_TYPES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<LimitManagementOperationType>> OPERATION_TYPE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<List<LimitManagementRule>>> RULES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<LimitManagementApiResponse<LimitManagementRule>> RULE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final LimitManagementProperties properties;
    private final RestClient restClient;

    public HttpLimitManagementAdapter(LimitManagementProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.baseUrl()))
                .requestFactory(requestFactory(properties))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<MerchantGroupType> listGroupTypes() {
        return call("limit-management group type list request failed", () -> {
            LimitManagementApiResponse<List<LimitManagementGroupType>> response = restClient.get()
                    .uri("/internal/v1/limit-management/merchant-group-types")
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUP_TYPES_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(LimitManagementGroupType::toDomain).toList();
        });
    }

    @Override
    public List<MerchantGroup> listGroups(UUID typeId) {
        return call("limit-management group list request failed", () -> {
            LimitManagementApiResponse<List<LimitManagementGroup>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/internal/v1/limit-management/merchant-groups");
                        if (typeId != null) {
                            uri.queryParam("typeId", typeId);
                        }
                        return uri.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUPS_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(LimitManagementGroup::toDomain).toList();
        });
    }

    @Override
    public List<MerchantGroupMembership> listMemberships(MembershipQuery query) {
        return call("limit-management membership list request failed", () -> {
            LimitManagementApiResponse<List<LimitManagementMembership>> response = restClient.get()
                    .uri(builder -> {
                        var uri = builder.path("/internal/v1/limit-management/merchant-group-memberships");
                        if (query.merchantId() != null && !query.merchantId().isBlank()) {
                            uri.queryParam("merchantId", query.merchantId());
                        }
                        if (query.typeId() != null) {
                            uri.queryParam("typeId", query.typeId());
                        }
                        if (query.groupId() != null) {
                            uri.queryParam("groupId", query.groupId());
                        }
                        if (query.state() != null && !query.state().isBlank()) {
                            uri.queryParam("state", query.state());
                        }
                        return uri.build();
                    })
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(MEMBERSHIPS_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(LimitManagementMembership::toDomain).toList();
        });
    }

    @Override
    public MerchantGroupType createGroupType(CreateGroupTypeCommand command) {
        return call("limit-management group type create request failed", () -> {
            LimitManagementApiResponse<LimitManagementGroupType> response = restClient.post()
                    .uri("/internal/v1/limit-management/merchant-group-types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUP_TYPE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public MerchantGroupType patchGroupType(UUID id, PatchGroupTypeCommand command) {
        return call("limit-management group type patch request failed", () -> {
            LimitManagementApiResponse<LimitManagementGroupType> response = restClient.patch()
                    .uri("/internal/v1/limit-management/merchant-group-types/{typeId}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUP_TYPE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public MerchantGroup createGroup(CreateGroupCommand command) {
        return call("limit-management group create request failed", () -> {
            LimitManagementApiResponse<LimitManagementGroup> response = restClient.post()
                    .uri("/internal/v1/limit-management/merchant-groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUP_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public MerchantGroup patchGroup(UUID id, PatchGroupCommand command) {
        return call("limit-management group patch request failed", () -> {
            LimitManagementApiResponse<LimitManagementGroup> response = restClient.patch()
                    .uri("/internal/v1/limit-management/merchant-groups/{groupId}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(GROUP_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public MerchantGroupMembership assignMembership(AssignMembershipCommand command) {
        return call("limit-management membership assign request failed", () -> {
            LimitManagementApiResponse<LimitManagementMembership> response = restClient.post()
                    .uri("/internal/v1/limit-management/merchant-group-memberships")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(MEMBERSHIP_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public MerchantGroupMembership closeMembership(UUID id, CloseMembershipCommand command) {
        return call("limit-management membership close request failed", () -> {
            LimitManagementApiResponse<LimitManagementMembership> response = restClient.post()
                    .uri("/internal/v1/limit-management/merchant-group-memberships/{membershipId}/close", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(MEMBERSHIP_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public List<OperationType> listOperationTypes() {
        return call("limit-management operation type list request failed", () -> {
            LimitManagementApiResponse<List<LimitManagementOperationType>> response = restClient.get()
                    .uri("/internal/v1/limit-management/operation-types")
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(OPERATION_TYPES_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(LimitManagementOperationType::toDomain).toList();
        });
    }

    @Override
    public OperationType createOperationType(CreateOperationTypeCommand command) {
        return call("limit-management operation type create request failed", () -> {
            LimitManagementApiResponse<LimitManagementOperationType> response = restClient.post()
                    .uri("/internal/v1/limit-management/operation-types")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(OPERATION_TYPE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public OperationType patchOperationType(UUID id, PatchOperationTypeCommand command) {
        return call("limit-management operation type patch request failed", () -> {
            LimitManagementApiResponse<LimitManagementOperationType> response = restClient.patch()
                    .uri("/internal/v1/limit-management/operation-types/{typeId}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(OPERATION_TYPE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public List<LimitRule> listRules() {
        return call("limit-management rule list request failed", () -> {
            LimitManagementApiResponse<List<LimitManagementRule>> response = restClient.get()
                    .uri("/internal/v1/limit-management/rules")
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULES_TYPE);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data().stream().map(LimitManagementRule::toDomain).toList();
        });
    }

    @Override
    public LimitRule createRule(CreateLimitRuleCommand command) {
        return call("limit-management rule create request failed", () -> {
            LimitManagementApiResponse<LimitManagementRule> response = restClient.post()
                    .uri("/internal/v1/limit-management/rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public LimitRule patchRule(UUID id, PatchLimitRuleCommand command) {
        return call("limit-management rule patch request failed", () -> {
            LimitManagementApiResponse<LimitManagementRule> response = restClient.patch()
                    .uri("/internal/v1/limit-management/rules/{ruleId}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public LimitRule activateRule(UUID id) {
        return call("limit-management rule activate request failed", () -> {
            LimitManagementApiResponse<LimitManagementRule> response = restClient.post()
                    .uri("/internal/v1/limit-management/rules/{ruleId}/activate", id)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public LimitRule disableRule(UUID id) {
        return call("limit-management rule disable request failed", () -> {
            LimitManagementApiResponse<LimitManagementRule> response = restClient.post()
                    .uri("/internal/v1/limit-management/rules/{ruleId}/disable", id)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    @Override
    public LimitRule createNewRuleVersion(UUID id) {
        return call("limit-management rule new version request failed", () -> {
            LimitManagementApiResponse<LimitManagementRule> response = restClient.post()
                    .uri("/internal/v1/limit-management/rules/{ruleId}/new-version", id)
                    .headers(this::addHeaders)
                    .retrieve()
                    .body(RULE_TYPE);
            return response == null || response.data() == null ? null : response.data().toDomain();
        });
    }

    private <T> T call(String message, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new UpstreamUnavailableException(message, ex);
        }
    }

    private JdkClientHttpRequestFactory requestFactory(LimitManagementProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }

    private void addHeaders(HttpHeaders headers) {
        if (!properties.internalAdminApiKey().isBlank()) {
            headers.set(properties.internalAdminHeaderName(), properties.internalAdminApiKey());
        }
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            headers.set(RequestIdFilter.HEADER_NAME, traceId);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
