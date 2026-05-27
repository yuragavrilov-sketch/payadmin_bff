package ru.copperside.payadmin.merchant.adapter.in.web;

import ru.copperside.payadmin.merchant.domain.AdminMerchant;

import java.time.Instant;

public record MerchantResponse(
        String id,
        String name,
        String status,
        String mcc,
        String inn,
        Instant createdAt
) {
    static MerchantResponse from(AdminMerchant merchant) {
        return new MerchantResponse(
                merchant.id(),
                merchant.name(),
                merchant.status().value(),
                merchant.mcc(),
                merchant.inn(),
                merchant.createdAt()
        );
    }
}

