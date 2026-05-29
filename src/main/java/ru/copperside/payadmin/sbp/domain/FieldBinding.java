package ru.copperside.payadmin.sbp.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldBinding(String name, String parent, String key, String path) {
}
