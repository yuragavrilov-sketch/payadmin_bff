package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreMerchantWithConfigLine(
        Long mercId,
        String name,
        Long hierarchyId,
        String initiator,
        String circuit,
        Map<String, String> configuration,
        Instant activeSince
) {
}

