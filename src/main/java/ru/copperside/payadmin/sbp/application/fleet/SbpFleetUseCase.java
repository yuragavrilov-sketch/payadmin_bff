package ru.copperside.payadmin.sbp.application.fleet;

import ru.copperside.payadmin.sbp.application.fleet.port.out.SbpFleetPort;
import ru.copperside.payadmin.sbp.domain.RouterFleet;

public class SbpFleetUseCase {

    private final SbpFleetPort port;

    public SbpFleetUseCase(SbpFleetPort port) {
        this.port = port;
    }

    public RouterFleet listRouters() {
        return port.listRouters();
    }
}
