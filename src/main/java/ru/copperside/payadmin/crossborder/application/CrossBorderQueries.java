package ru.copperside.payadmin.crossborder.application;

import ru.copperside.payadmin.crossborder.application.port.out.CrossBorderEnginePort;
import ru.copperside.payadmin.crossborder.domain.OperationsPage;
import ru.copperside.payadmin.crossborder.domain.PartnerCountry;
import ru.copperside.payadmin.crossborder.domain.TransferSettings;
import ru.copperside.payadmin.crossborder.domain.TransferSettingsUpdate;

import java.util.List;

public class CrossBorderQueries {

    private final CrossBorderEnginePort port;

    public CrossBorderQueries(CrossBorderEnginePort port) {
        this.port = port;
    }

    public List<PartnerCountry> listBanks() {
        return port.listBanks();
    }

    public OperationsPage listOperations(int limit, int offset) {
        return port.listOperations(limit, offset);
    }

    public TransferSettings getSettings() {
        return port.getSettings();
    }

    public TransferSettings updateSettings(TransferSettingsUpdate update) {
        return port.updateSettings(update);
    }
}
