package ru.copperside.payadmin.sbp.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.sbp.application.fleet.SbpFleetUseCase;
import ru.copperside.payadmin.sbp.domain.RouterFleet;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/sbp/routers")
public class SbpFleetController {

    private final SbpFleetUseCase useCase;
    private final Clock clock;

    public SbpFleetController(SbpFleetUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<RouterFleet> list() {
        return ApiResponse.success(useCase.listRouters(), clock);
    }
}
