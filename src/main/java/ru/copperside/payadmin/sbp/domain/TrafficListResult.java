package ru.copperside.payadmin.sbp.domain;

import java.util.List;

public record TrafficListResult(List<TrafficTransaction> items, long total, int page, int size) {
}
