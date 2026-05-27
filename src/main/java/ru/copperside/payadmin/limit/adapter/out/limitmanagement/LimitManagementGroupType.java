package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import ru.copperside.payadmin.limit.domain.MerchantGroupType;

import java.time.Instant;
import java.util.UUID;

record LimitManagementGroupType(
        UUID id,
        String code,
        String name,
        String description,
        boolean enabled,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    MerchantGroupType toDomain() {
        return new MerchantGroupType(id, code, name, description, enabled, sortOrder, createdAt, updatedAt);
    }
}
