package ru.copperside.payadmin.limit.domain;

import java.time.Instant;
import java.util.UUID;

public record MerchantGroupMembership(
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
}
