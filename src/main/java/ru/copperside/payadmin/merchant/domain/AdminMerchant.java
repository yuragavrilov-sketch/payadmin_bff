package ru.copperside.payadmin.merchant.domain;

import java.time.Instant;

public record AdminMerchant(
        String id,
        String name,
        MerchantStatus status,
        String mcc,
        Instant createdAt
) {
}

