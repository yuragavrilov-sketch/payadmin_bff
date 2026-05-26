package ru.copperside.payadmin.merchant.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiMeta;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.merchant.application.ListMerchantsUseCase;
import ru.copperside.payadmin.merchant.domain.MerchantQuery;
import ru.copperside.payadmin.merchant.domain.MerchantSortField;
import ru.copperside.payadmin.merchant.domain.MerchantStatus;
import ru.copperside.payadmin.merchant.domain.PageWindow;
import ru.copperside.payadmin.merchant.domain.SearchTerm;
import ru.copperside.payadmin.merchant.domain.SortDirection;
import ru.copperside.payadmin.merchant.domain.SortOrder;

import java.time.Clock;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_SORT_BY = "id";
    private static final String DEFAULT_SORT_DIR = "asc";

    private final ListMerchantsUseCase listMerchantsUseCase;
    private final Clock clock;

    public MerchantController(ListMerchantsUseCase listMerchantsUseCase, Clock clock) {
        this.listMerchantsUseCase = listMerchantsUseCase;
        this.clock = clock;
    }

    @GetMapping
    public ApiResponse<List<MerchantResponse>> listMerchants(
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) @Min(1) @Max(PageWindow.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = "" + DEFAULT_OFFSET) @Min(0) int offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIR) String sortDir
    ) {
        SearchTerm searchTerm = SearchTerm.of(search);
        MerchantStatus merchantStatus = parseStatus(status);
        MerchantSortField sortField = MerchantSortField.from(sortBy);
        SortDirection sortDirection = SortDirection.from(sortDir);
        MerchantQuery query = new MerchantQuery(
                PageWindow.of(limit, offset),
                searchTerm,
                merchantStatus,
                SortOrder.of(sortField, sortDirection)
        );

        ListMerchantsUseCase.MerchantPage page = listMerchantsUseCase.list(query);
        List<MerchantResponse> data = page.data().stream()
                .map(MerchantResponse::from)
                .toList();

        ApiMeta meta = new ApiMeta(
                limit,
                offset,
                page.count(),
                page.total(),
                searchTerm.isPresent() ? searchTerm.value() : null,
                merchantStatus == null ? null : merchantStatus.value(),
                sortField.value(),
                sortDirection.value()
        );
        return ApiResponse.success(data, meta, clock);
    }

    private MerchantStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return MerchantStatus.from(status);
    }
}

