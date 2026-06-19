package ru.copperside.payadmin.crossborder.application.port.out;

import tools.jackson.databind.JsonNode;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.util.List;

public interface CrossBorderEnginePort {
    List<PartnerCountry> listBanks();
    OperationsPage listOperations(int limit, int offset);
    TransferSettings getSettings();
    TransferSettings updateSettings(TransferSettingsUpdate update);

    /**
     * Тестовый passthrough в payout-методы engine (op ∈ convert|create|get).
     * Тело и ответ — сырой JSON AsiaPay; только для тест-страницы админ-консоли.
     */
    JsonNode proxyPayout(String op, JsonNode body);
}
