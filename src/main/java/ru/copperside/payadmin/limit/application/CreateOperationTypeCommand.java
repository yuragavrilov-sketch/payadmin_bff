package ru.copperside.payadmin.limit.application;

import ru.copperside.payadmin.limit.domain.OperationDirection;

public record CreateOperationTypeCommand(
        String code,
        String name,
        String familyCode,
        OperationDirection direction
) {
}
