package ru.copperside.payadmin.merchant.application.port.out;

import java.time.Instant;

public record MerchantConfigurationEntry(
        String parameterName,
        String parameterValue,
        Instant dateBegin,
        Instant dateEnd
) {
}

