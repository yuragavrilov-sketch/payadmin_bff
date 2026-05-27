package ru.copperside.payadmin.terminal.application.port.out;

public record TerminalLine(
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
