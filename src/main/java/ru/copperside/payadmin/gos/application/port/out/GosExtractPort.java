package ru.copperside.payadmin.gos.application.port.out;

import ru.copperside.payadmin.gos.application.GosExtractCommand;
import ru.copperside.payadmin.gos.application.GosExtractResult;
import ru.copperside.payadmin.gos.application.GosExtractStatusQuery;
import ru.copperside.payadmin.gos.application.GosExtractStatusResult;

public interface GosExtractPort {
    GosExtractResult requestExtract(GosExtractCommand command);

    GosExtractStatusResult getStatus(GosExtractStatusQuery query);
}
