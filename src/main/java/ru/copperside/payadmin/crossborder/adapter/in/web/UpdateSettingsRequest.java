package ru.copperside.payadmin.crossborder.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.math.BigDecimal;

public record UpdateSettingsRequest(
        Long walletId,
        BigDecimal ratePercent,
        BigDecimal senderPercent,
        BigDecimal senderMinCommission,
        @NotBlank String defaultSenderCurrency,
        @NotBlank String defaultReceiverCurrency,
        @NotBlank String defaultPayoutMethod,
        boolean testMode
) {
    public TransferSettingsUpdate toDomain() {
        return new TransferSettingsUpdate(walletId, ratePercent, senderPercent, senderMinCommission,
                defaultSenderCurrency, defaultReceiverCurrency, defaultPayoutMethod, testMode);
    }
}
