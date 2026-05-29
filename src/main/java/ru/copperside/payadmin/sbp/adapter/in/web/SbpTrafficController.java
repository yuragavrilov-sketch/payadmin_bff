package ru.copperside.payadmin.sbp.adapter.in.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.sbp.application.traffic.SbpTrafficUseCase;
import ru.copperside.payadmin.sbp.application.traffic.TrafficQuery;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;
import ru.copperside.payadmin.sbp.domain.TrafficTransaction;

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/sbp/traffic")
public class SbpTrafficController {

    private final SbpTrafficUseCase useCase;
    private final Clock clock;

    public SbpTrafficController(SbpTrafficUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping("/transactions")
    public ApiResponse<TrafficListResult> list(
            @RequestParam(required = false) String requestType,
            @RequestParam(required = false) String terminalOwner,
            @RequestParam(required = false) String upstream,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        TrafficQuery query = new TrafficQuery(requestType, terminalOwner, upstream, outcome, status, from, to, q, page, size);
        return ApiResponse.success(useCase.listTransactions(query), clock);
    }

    @GetMapping("/transactions/{correlationId}")
    public ApiResponse<TrafficTransaction> get(@PathVariable String correlationId) {
        return ApiResponse.success(useCase.getTransaction(correlationId), clock);
    }

    @GetMapping("/stats")
    public ApiResponse<TrafficStats> stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.success(useCase.stats(from, to), clock);
    }
}
