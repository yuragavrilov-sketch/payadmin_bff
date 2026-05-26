package ru.copperside.payadmin.merchant.domain;

public record MerchantQuery(
        PageWindow page,
        SearchTerm search,
        MerchantStatus status,
        SortOrder<MerchantSortField> sort
) {
}

