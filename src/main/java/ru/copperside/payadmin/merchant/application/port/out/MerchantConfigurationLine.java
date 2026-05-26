package ru.copperside.payadmin.merchant.application.port.out;

import java.time.Instant;
import java.util.Map;

public record MerchantConfigurationLine(
        Long mercId,
        String name,
        Map<String, String> configuration,
        Instant activeSince
) {
}

