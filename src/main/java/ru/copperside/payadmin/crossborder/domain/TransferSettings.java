package ru.copperside.payadmin.crossborder.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferSettings(
        Long walletId,
        BigDecimal ratePercent,
        BigDecimal senderPercent,
        BigDecimal senderMinCommission,
        String defaultSenderCurrency,
        String defaultReceiverCurrency,
        String defaultPayoutMethod,
        boolean testMode,
        Instant updatedAt,
        String updatedBy
) {
}
