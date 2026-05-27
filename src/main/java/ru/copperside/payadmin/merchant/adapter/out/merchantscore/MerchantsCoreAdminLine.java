package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreAdminLine(
        Long mercId,
        String name,
        String status,
        String mcc,
        String inn,
        Instant createdAt
) {
}
