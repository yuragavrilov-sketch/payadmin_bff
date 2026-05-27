package ru.copperside.payadmin.terminal.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiMeta;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.merchant.domain.PageWindow;
import ru.copperside.payadmin.merchant.domain.SearchTerm;
import ru.copperside.payadmin.merchant.domain.SortDirection;
import ru.copperside.payadmin.merchant.domain.SortOrder;
import ru.copperside.payadmin.terminal.application.ListTerminalsUseCase;
import ru.copperside.payadmin.terminal.domain.TerminalQuery;
import ru.copperside.payadmin.terminal.domain.TerminalSortField;

import java.time.Clock;
import java.util.List;

@Validated
@RestController
public class TerminalController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_SORT_BY = "mercId";
    private static final String DEFAULT_SORT_DIR = "asc";

    private final ListTerminalsUseCase listTerminalsUseCase;
    private final Clock clock;

    public TerminalController(ListTerminalsUseCase listTerminalsUseCase, Clock clock) {
        this.listTerminalsUseCase = listTerminalsUseCase;
        this.clock = clock;
    }

    @GetMapping("/api/v1/terminals")
    public ApiResponse<List<TerminalResponse>> listTerminals(
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "" + DEFAULT_OFFSET) @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir
    ) {
        TerminalQuery query = buildQuery(limit, offset, search, sortBy, sortDir);
        ListTerminalsUseCase.TerminalPage page = listTerminalsUseCase.list(query);
        return envelope(page, limit, offset, search, sortBy, sortDir);
    }

    @GetMapping("/api/v1/merchants/{merchantId}/terminals")
    public ApiResponse<List<TerminalResponse>> listMerchantTerminals(
            @PathVariable long merchantId,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "" + DEFAULT_OFFSET) @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir
    ) {
        TerminalQuery query = buildQuery(limit, offset, search, sortBy, sortDir);
        ListTerminalsUseCase.TerminalPage page = listTerminalsUseCase.listByMerchant(merchantId, query);
        return envelope(page, limit, offset, search, sortBy, sortDir);
    }

    private TerminalQuery buildQuery(int limit, int offset, String search, String sortBy, String sortDir) {
        return new TerminalQuery(
                PageWindow.of(limit, offset),
                SearchTerm.of(search),
                SortOrder.of(TerminalSortField.from(sortBy), SortDirection.from(sortDir)));
    }

    private ApiResponse<List<TerminalResponse>> envelope(
            ListTerminalsUseCase.TerminalPage page, int limit, int offset, String search, String sortBy, String sortDir) {
        List<TerminalResponse> data = page.data().stream().map(TerminalResponse::from).toList();
        ApiMeta meta = new ApiMeta(limit, offset, page.count(), page.total(),
                (search == null || search.isBlank()) ? null : search, null, sortBy, sortDir);
        return ApiResponse.success(data, meta, clock);
    }
}
