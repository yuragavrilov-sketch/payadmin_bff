package ru.copperside.payadmin.limitprojection.application;

import ru.copperside.payadmin.limitprojection.application.port.out.ReservationProjectionPort;
import ru.copperside.payadmin.limitprojection.domain.ReservationEvent;
import ru.copperside.payadmin.limitprojection.domain.ReservationState;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;
import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ReservationProjectionUseCase {

    private static final int MAX_SIZE = 200;

    private final ReservationProjectionPort port;

    public ReservationProjectionUseCase(ReservationProjectionPort port) {
        this.port = Objects.requireNonNull(port, "port must not be null");
    }

    public ReservationStatePage list(String merchantId, String state, String from, String to, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 50 : Math.min(size, MAX_SIZE);
        return port.list(merchantId, state, from, to, safePage, safeSize);
    }

    public Optional<ReservationState> findByReservationId(String reservationId) {
        return port.findByReservationId(reservationId);
    }

    public Optional<ReservationState> findByOperationId(String operationId) {
        return port.findByOperationId(operationId);
    }

    public List<ReservationEvent> events(String reservationId) {
        return port.events(reservationId);
    }

    public List<ReservationSummaryRow> summary(String merchantId, String from, String to, String groupBy) {
        String safeGroupBy = "day".equalsIgnoreCase(groupBy) ? "day" : "merchant";
        return port.summary(merchantId, from, to, safeGroupBy);
    }
}
