package ru.copperside.payadmin.crossborder.application.port.out;

import ru.copperside.payadmin.crossborder.domain.CrossBorderOperation;
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
}
