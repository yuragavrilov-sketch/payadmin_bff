package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import ru.copperside.payadmin.limit.domain.MerchantGroup;

import java.time.Instant;
import java.util.UUID;

record LimitManagementGroup(
        UUID id,
        UUID typeId,
        String code,
        String name,
        String description,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    MerchantGroup toDomain() {
        return new MerchantGroup(id, typeId, code, name, description, enabled, createdAt, updatedAt);
    }
}
