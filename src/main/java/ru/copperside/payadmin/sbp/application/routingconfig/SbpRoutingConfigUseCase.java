package ru.copperside.payadmin.sbp.application.routingconfig;

import ru.copperside.payadmin.sbp.application.routingconfig.port.out.SbpRoutingConfigPort;
import ru.copperside.payadmin.sbp.domain.RoutingConfig;

public class SbpRoutingConfigUseCase {

    private final SbpRoutingConfigPort port;

    public SbpRoutingConfigUseCase(SbpRoutingConfigPort port) {
        this.port = port;
    }

    public RoutingConfig get() {
        return port.get();
    }

    public RoutingConfig replace(RoutingConfig config) {
        return port.replace(config);
    }
}
