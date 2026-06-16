package ru.copperside.payadmin.sbp.application.routingconfig.port.out;

import ru.copperside.payadmin.sbp.domain.RoutingConfig;

public interface SbpRoutingConfigPort {
    RoutingConfig get();
    RoutingConfig replace(RoutingConfig config);
}
