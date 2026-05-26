package ru.copperside.payadmin.merchant.application.port.out;

import java.time.Instant;

public record MerchantAdminLine(
        Long mercId,
        String name,
        String status,
        String mcc,
        Instant createdAt
) {
}
