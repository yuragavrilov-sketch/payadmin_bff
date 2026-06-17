package ru.copperside.payadmin.limitprojection.domain;

import java.util.List;

public record ReservationStatePage(List<ReservationState> items, int page, int size) {
}
