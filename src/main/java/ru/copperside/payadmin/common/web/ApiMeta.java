package ru.copperside.payadmin.common.web;

public record ApiMeta(
        Integer limit,
        Integer offset,
        Integer count,
        String search,
        String status,
        String sortBy,
        String sortDir
) {
}

