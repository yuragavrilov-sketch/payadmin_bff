package ru.copperside.payadmin.crossborder.domain;

import java.util.List;

public record PartnerProvider(
        String providerId,
        String providerName,
        String providerNameEn,
        List<PartnerMethod> methods
) {
}
