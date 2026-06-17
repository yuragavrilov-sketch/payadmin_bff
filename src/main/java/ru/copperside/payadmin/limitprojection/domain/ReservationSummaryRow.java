package ru.copperside.payadmin.limitprojection.domain;

public record ReservationSummaryRow(
        String groupKey,
        long confirmedCount,
        String confirmedAmount,
        String currency
) {
}
