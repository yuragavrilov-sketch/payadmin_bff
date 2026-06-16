package ru.copperside.payadmin.sbp.domain;

import java.util.List;
import java.util.Map;

/** The sbp-router routing config (backend groups + active group) owned by sbp-router-management. */
public record RoutingConfig(Long version, String activeGroup, Map<String, Group> groups) {

    public record Group(List<String> backends) {
    }
}
