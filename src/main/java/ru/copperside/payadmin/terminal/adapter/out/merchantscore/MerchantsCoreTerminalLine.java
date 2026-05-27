package ru.copperside.payadmin.terminal.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreTerminalLine(
        Long mercId,
        String mps,
        String gate,
        boolean is3ds,
        String terminalId,
        String merchantId,
        String mcc,
        String name,
        String merchantUrl,
        String login,
        boolean hasPassword,
        String apiUrl,
        String merchantName
) {
}
