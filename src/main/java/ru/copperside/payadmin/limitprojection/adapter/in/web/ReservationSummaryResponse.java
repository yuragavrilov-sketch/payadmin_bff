package ru.copperside.payadmin.limitprojection.adapter.in.web;

import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

public record ReservationSummaryResponse(
        String groupKey,
        long confirmedCount,
        String confirmedAmount,
        String currency
) {
    public static ReservationSummaryResponse from(ReservationSummaryRow r) {
        return new ReservationSummaryResponse(r.groupKey(), r.confirmedCount(), r.confirmedAmount(), r.currency());
    }
}
