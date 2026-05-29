package ru.copperside.payadmin.sbp.application.traffic;

import ru.copperside.payadmin.sbp.application.traffic.port.out.SbpTrafficPort;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;
import ru.copperside.payadmin.sbp.domain.TrafficTransaction;

import java.time.Instant;

public class SbpTrafficUseCase {

    private final SbpTrafficPort port;

    public SbpTrafficUseCase(SbpTrafficPort port) {
        this.port = port;
    }

    public TrafficListResult listTransactions(TrafficQuery query) { return port.listTransactions(query); }
    public TrafficTransaction getTransaction(String correlationId) { return port.getTransaction(correlationId); }
    public TrafficStats stats(Instant from, Instant to) { return port.stats(from, to); }
}
