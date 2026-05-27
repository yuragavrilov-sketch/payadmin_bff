package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import ru.copperside.payadmin.limit.domain.MerchantGroupMembership;

import java.time.Instant;
import java.util.UUID;

record LimitManagementMembership(
        UUID id,
        String merchantId,
        UUID groupId,
        UUID groupTypeId,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        String createdBy,
        Instant closedAt,
        String closedBy
) {
    MerchantGroupMembership toDomain() {
        return new MerchantGroupMembership(
                id,
                merchantId,
                groupId,
                groupTypeId,
                validFrom,
                validTo,
                createdAt,
                createdBy,
                closedAt,
                closedBy
        );
    }
}
