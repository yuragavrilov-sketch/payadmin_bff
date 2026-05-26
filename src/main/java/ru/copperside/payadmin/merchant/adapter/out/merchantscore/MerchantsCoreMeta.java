package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreMeta(
        Integer limit,
        Integer offset,
        Integer count,
        Long total,
        String search,
        String sortBy,
        String sortDir,
        String at
) {
}

