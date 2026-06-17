package ru.copperside.payadmin.crossborder.domain;

import java.math.BigDecimal;

public record TransferSettingsUpdate(
        Long walletId,
        BigDecimal ratePercent,
        BigDecimal senderPercent,
        BigDecimal senderMinCommission,
        String defaultSenderCurrency,
        String defaultReceiverCurrency,
        String defaultPayoutMethod,
        boolean testMode
) {
}
