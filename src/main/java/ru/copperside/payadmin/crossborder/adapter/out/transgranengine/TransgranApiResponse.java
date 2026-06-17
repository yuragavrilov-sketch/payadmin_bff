package ru.copperside.payadmin.crossborder.adapter.out.transgranengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransgranApiResponse<T>(T data, TransgranMeta meta, Object error, String timestamp) {
}
