package ru.copperside.payadmin.limit.adapter.in.web;

import ru.copperside.payadmin.limit.domain.OperationType;

import java.time.Instant;
import java.util.UUID;

public record OperationTypeResponse(
        UUID id,
        String code,
        String name,
        String familyCode,
        String direction,
        boolean enabled,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    static OperationTypeResponse from(OperationType type) {
        return new OperationTypeResponse(
                type.id(),
                type.code(),
                type.name(),
                type.familyCode(),
                type.direction() == null ? null : type.direction().name(),
                type.enabled(),
                type.sortOrder(),
                type.createdAt(),
                type.updatedAt()
        );
    }
}
