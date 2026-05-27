package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.OperationDirection;

public record PatchOperationTypeCommand(
        String name,
        String familyCode,
        OperationDirection direction,
        Boolean enabled
) {
}
