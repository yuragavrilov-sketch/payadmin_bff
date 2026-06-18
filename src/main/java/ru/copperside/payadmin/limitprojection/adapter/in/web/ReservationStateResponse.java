package ru.copperside.payadmin.limitprojection.adapter.in.web;

import ru.copperside.payadmin.limitprojection.domain.ReservationState;

public record ReservationStateResponse(
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
    public static ReservationStateResponse from(ReservationState s) {
        return new ReservationStateResponse(s.reservationId(), s.operationId(), s.state(), s.merchantId(),
                s.operationType(), s.direction(), s.amount(), s.currency(), s.heldAt(), s.lastOccurredAt(), s.staleAfter());
    }
}
