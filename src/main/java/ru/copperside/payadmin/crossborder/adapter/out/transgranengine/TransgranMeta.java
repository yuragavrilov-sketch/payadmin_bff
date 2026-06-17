package ru.copperside.payadmin.crossborder.adapter.out.transgranengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransgranMeta(Integer limit, Integer offset, Integer count, Long total) {
}
