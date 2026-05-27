package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

import ru.copperside.payadmin.limit.domain.OperationDirection;
import ru.copperside.payadmin.limit.domain.OperationType;

import java.time.Instant;
import java.util.UUID;

record LimitManagementOperationType(
        UUID id,
        String code,
        String name,
        String familyCode,
        OperationDirection direction,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    OperationType toDomain() {
        return new OperationType(id, code, name, familyCode, direction, enabled, createdAt, updatedAt);
    }
}
