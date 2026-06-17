package ru.copperside.payadmin.crossborder.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CrossBorderOperation(
        String id,
        String requestId,
        String type,
        String status,
        String senderCurrency,
        BigDecimal senderAmount,
        String receiverCurrency,
        BigDecimal receiverAmount,
        BigDecimal ratePercent,
        String payoutMethod,
        String payoutType,
        Long walletId,
        Boolean isTest,
        String expiredDate,
        Instant createdAt
) {
}
