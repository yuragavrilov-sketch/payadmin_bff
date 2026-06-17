package ru.copperside.payadmin.limitprojection.application.port.out;

import ru.copperside.payadmin.limitprojection.domain.ReservationEvent;
import ru.copperside.payadmin.limitprojection.domain.ReservationState;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;
import ru.copperside.payadmin.limitprojection.domain.ReservationSummaryRow;

import java.util.List;
import java.util.Optional;

public interface ReservationProjectionPort {

    ReservationStatePage list(String merchantId, String state, String from, String to, int page, int size);

    Optional<ReservationState> findByReservationId(String reservationId);

    Optional<ReservationState> findByOperationId(String operationId);

    List<ReservationEvent> events(String reservationId);

    List<ReservationSummaryRow> summary(String merchantId, String from, String to, String groupBy);
}
