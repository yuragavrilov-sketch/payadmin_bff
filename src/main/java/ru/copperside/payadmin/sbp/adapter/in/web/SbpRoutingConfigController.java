package ru.copperside.payadmin.sbp.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.sbp.application.routingconfig.SbpRoutingConfigUseCase;
import ru.copperside.payadmin.sbp.domain.RoutingConfig;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/sbp/routing-config")
public class SbpRoutingConfigController {

    private final SbpRoutingConfigUseCase useCase;
    private final Clock clock;

    public SbpRoutingConfigController(SbpRoutingConfigUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<RoutingConfig> get() {
        return ApiResponse.success(useCase.get(), clock);
    }

    @PutMapping
    public ApiResponse<RoutingConfig> put(@RequestBody RoutingConfig config) {
        return ApiResponse.success(useCase.replace(config), clock);
    }
}
