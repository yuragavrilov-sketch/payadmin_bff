package ru.copperside.payadmin.limitprojection.domain;

public record ReservationState(
        String reservationId,
        String operationId,
        String state,
        String merchantId,
        String operationType,
        String direction,
        String amount,
        String currency,
        String heldAt,
        String lastOccurredAt,
        String staleAfter
) {
}
