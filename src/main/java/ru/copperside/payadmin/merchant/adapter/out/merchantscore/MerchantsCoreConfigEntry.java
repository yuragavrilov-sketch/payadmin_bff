package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreConfigEntry(
        String parameterName,
        String parameterValue,
        Instant dateBegin,
        Instant dateEnd
) {
}

