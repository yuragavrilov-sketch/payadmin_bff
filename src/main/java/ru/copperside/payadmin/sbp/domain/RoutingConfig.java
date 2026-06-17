package ru.copperside.payadmin.sbp.domain;

import java.util.List;
import java.util.Map;

/** The sbp-router routing config (backend groups + active group + optional AuthPay route). */
public record RoutingConfig(Long version, String activeGroup, Map<String, Group> groups, AuthPay authPay) {

    public record Group(List<String> backends) {
    }

    public record AuthPay(boolean enabled, List<String> backends, Integer timeoutMs) {
    }

    /** Backward-compatible constructor for call sites that predate the authPay route. */
    public RoutingConfig(Long version, String activeGroup, Map<String, Group> groups) {
        this(version, activeGroup, groups, null);
    }
}
