package ru.copperside.payadmin.limitprojection.adapter.in.web;

import ru.copperside.payadmin.limitprojection.domain.ReservationEvent;

public record ReservationEventResponse(
        String eventId,
        String eventType,
        String state,
        String occurredAt,
        String amount,
        String currency
) {
    public static ReservationEventResponse from(ReservationEvent e) {
        return new ReservationEventResponse(e.eventId(), e.eventType(), e.state(), e.occurredAt(), e.amount(), e.currency());
    }
}
