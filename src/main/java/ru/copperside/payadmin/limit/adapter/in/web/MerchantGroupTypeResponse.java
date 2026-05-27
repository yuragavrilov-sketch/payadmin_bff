package ru.copperside.payadmin.limit.adapter.in.web;

import ru.copperside.payadmin.limit.domain.MerchantGroupType;

import java.time.Instant;
import java.util.UUID;

public record MerchantGroupTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean enabled,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    static MerchantGroupTypeResponse from(MerchantGroupType type) {
        return new MerchantGroupTypeResponse(
                type.id(),
                type.code(),
                type.name(),
                type.description(),
                type.enabled(),
                type.sortOrder(),
                type.createdAt(),
                type.updatedAt()
        );
    }
}
