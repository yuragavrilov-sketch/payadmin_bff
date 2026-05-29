package ru.copperside.payadmin.sbp.application.traffic.port.out;

import ru.copperside.payadmin.sbp.application.traffic.TrafficQuery;
import ru.copperside.payadmin.sbp.domain.TrafficListResult;
import ru.copperside.payadmin.sbp.domain.TrafficStats;
import ru.copperside.payadmin.sbp.domain.TrafficTransaction;

import java.time.Instant;

public interface SbpTrafficPort {
    TrafficListResult listTransactions(TrafficQuery query);
    TrafficTransaction getTransaction(String correlationId);
    TrafficStats stats(Instant from, Instant to);
}
