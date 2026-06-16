package ru.copperside.payadmin.gos.application;

import ru.copperside.payadmin.gos.application.port.out.GosExtractPort;

public class GosExtractUseCase {

    private final GosExtractPort port;

    public GosExtractUseCase(GosExtractPort port) {
        this.port = port;
    }

    public GosExtractResult requestExtract(GosExtractCommand command) {
        return port.requestExtract(command);
    }

    public GosExtractStatusResult getStatus(GosExtractStatusQuery query) {
        return port.getStatus(query);
    }
}
