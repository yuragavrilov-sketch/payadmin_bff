package ru.copperside.payadmin.crossborder.domain;

import java.util.List;

public record PartnerMethod(
        String method,
        Long walletId,
        String walletCurrency,
        List<String> supportedCurrencies,
        List<ProviderRequiredField> requiredFields
) {
}
