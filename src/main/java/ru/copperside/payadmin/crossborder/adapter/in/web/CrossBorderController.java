package ru.copperside.payadmin.crossborder.adapter.in.web;

import tools.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiMeta;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.crossborder.application.CrossBorderQueries;
import ru.copperside.payadmin.crossborder.domain.CrossBorderOperation;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/v1/crossborder")
public class CrossBorderController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_OFFSET = 0;

    private final CrossBorderQueries queries;
    private final Clock clock;

    public CrossBorderController(CrossBorderQueries queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    @GetMapping("/banks")
    public ApiResponse<List<PartnerCountry>> banks() {
        return ApiResponse.success(queries.listBanks(), clock);
    }

    @GetMapping("/operations")
    public ApiResponse<List<CrossBorderOperation>> operations(
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Min(1) @Max(500) int limit,
            @RequestParam(defaultValue = "" + DEFAULT_OFFSET) @Min(0) int offset) {
        OperationsPage page = queries.listOperations(limit, offset);
        ApiMeta meta = new ApiMeta(limit, offset, page.data().size(), page.total(), null, null, null, null);
        return ApiResponse.success(page.data(), meta, clock);
    }

    @GetMapping("/settings")
    public ApiResponse<TransferSettings> getSettings() {
        return ApiResponse.success(queries.getSettings(), clock);
    }

    @PutMapping("/settings")
    public ApiResponse<TransferSettings> updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return ApiResponse.success(queries.updateSettings(request.toDomain()), clock);
    }

    private static final Set<String> PAYOUT_OPS = Set.of("convert", "create", "get", "confirm");

    /**
     * Тестовый passthrough в payout-методы engine (только для тест-страницы админ-консоли).
     * op ∈ convert|create|get; тело и ответ — сырой JSON AsiaPay.
     */
    @PostMapping("/test/{op}")
    public ApiResponse<JsonNode> testPayout(@PathVariable String op, @RequestBody JsonNode body) {
        if (!PAYOUT_OPS.contains(op)) {
            throw new IllegalArgumentException("unknown payout op: " + op);
        }
        return ApiResponse.success(queries.proxyPayout(op, body), clock);
    }
}
