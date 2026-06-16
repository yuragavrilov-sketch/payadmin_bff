package ru.copperside.payadmin.gos.adapter.in.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.gos.application.GosExtractCommand;
import ru.copperside.payadmin.gos.application.GosExtractResult;
import ru.copperside.payadmin.gos.application.GosExtractStatusQuery;
import ru.copperside.payadmin.gos.application.GosExtractStatusResult;
import ru.copperside.payadmin.gos.application.GosExtractUseCase;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/gos/egrul-egrip")
public class GosExtractController {

    private final GosExtractUseCase useCase;
    private final Clock clock;

    public GosExtractController(GosExtractUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @PostMapping("/getExtract")
    public ApiResponse<GosExtractResult> getExtract(@RequestBody GosExtractRequest request) {
        GosExtractCommand command = new GosExtractCommand(
                request.merchId(), request.beneficiaryType(), request.inn(), request.name());
        return ApiResponse.success(useCase.requestExtract(command), clock);
    }

    @PostMapping("/getExtractStatus")
    public ApiResponse<GosExtractStatusResult> getExtractStatus(@RequestBody GosExtractStatusRequest request) {
        GosExtractStatusQuery query = new GosExtractStatusQuery(request.merchId(), request.extractRequestId());
        return ApiResponse.success(useCase.getStatus(query), clock);
    }
}
