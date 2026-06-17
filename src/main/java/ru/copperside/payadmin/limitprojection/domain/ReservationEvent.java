package ru.copperside.payadmin.limitprojection.domain;

public record ReservationEvent(
        String eventId,
        String eventType,
        String state,
        String occurredAt,
        String amount,
        String currency
) {
}
