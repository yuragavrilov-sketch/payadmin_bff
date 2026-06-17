package ru.copperside.payadmin.crossborder.domain;

import java.util.List;

public record PartnerCountry(
        String countryCode,
        String countryName,
        String countryNameEn,
        List<PartnerProvider> providers
) {
}
