package ru.copperside.payadmin.crossborder.domain;

import java.util.List;

public record OperationsPage(
        List<CrossBorderOperation> data,
        long total
) {
}
