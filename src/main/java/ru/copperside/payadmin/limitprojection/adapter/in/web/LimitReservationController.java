package ru.copperside.payadmin.limitprojection.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiMeta;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.common.web.NotFoundException;
import ru.copperside.payadmin.limitprojection.application.ReservationProjectionUseCase;
import ru.copperside.payadmin.limitprojection.domain.ReservationStatePage;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/api/v1/limit-reservations")
public class LimitReservationController {

    private final ReservationProjectionUseCase useCase;
    private final Clock clock;

    public LimitReservationController(ReservationProjectionUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<ReservationStateResponse>> list(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        ReservationStatePage result = useCase.list(merchantId, state, from, to, page, size);
        List<ReservationStateResponse> data = result.items().stream().map(ReservationStateResponse::from).toList();
        ApiMeta meta = new ApiMeta(result.size(), (result.page() - 1) * result.size(), data.size(), null, null, null, null, null);
        return ApiResponse.success(data, meta, clock);
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationStateResponse> byReservationId(@PathVariable String reservationId) {
        ReservationStateResponse data = useCase.findByReservationId(reservationId)
                .map(ReservationStateResponse::from)
                .orElseThrow(() -> new NotFoundException("reservation " + reservationId + " not found"));
        return ApiResponse.success(data, clock);
    }

    @GetMapping("/by-operation/{operationId}")
    public ApiResponse<ReservationStateResponse> byOperationId(@PathVariable String operationId) {
        ReservationStateResponse data = useCase.findByOperationId(operationId)
                .map(ReservationStateResponse::from)
                .orElseThrow(() -> new NotFoundException("reservation for operation " + operationId + " not found"));
        return ApiResponse.success(data, clock);
    }

    @GetMapping("/{reservationId}/events")
    public ApiResponse<List<ReservationEventResponse>> events(@PathVariable String reservationId) {
        List<ReservationEventResponse> data = useCase.events(reservationId).stream()
                .map(ReservationEventResponse::from).toList();
        return ApiResponse.success(data, clock);
    }

    @GetMapping("/summary")
    public ApiResponse<List<ReservationSummaryResponse>> summary(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "merchant") String groupBy
    ) {
        List<ReservationSummaryResponse> data = useCase.summary(merchantId, from, to, groupBy).stream()
                .map(ReservationSummaryResponse::from).toList();
        return ApiResponse.success(data, clock);
    }
}
