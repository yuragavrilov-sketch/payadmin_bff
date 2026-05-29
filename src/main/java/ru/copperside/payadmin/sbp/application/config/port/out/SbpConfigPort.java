package ru.copperside.payadmin.sbp.application.config.port.out;

import ru.copperside.payadmin.sbp.application.config.ExtractionRuleRequest;
import ru.copperside.payadmin.sbp.application.config.RoutingFlagRequest;
import ru.copperside.payadmin.sbp.application.config.TerminalConfigRequest;
import ru.copperside.payadmin.sbp.application.config.TkbPayEntryRequest;
import ru.copperside.payadmin.sbp.application.config.UpstreamRequest;
import ru.copperside.payadmin.sbp.domain.ExtractionRule;
import ru.copperside.payadmin.sbp.domain.PendingChanges;
import ru.copperside.payadmin.sbp.domain.RoutingFlag;
import ru.copperside.payadmin.sbp.domain.RoutingManifest;
import ru.copperside.payadmin.sbp.domain.TerminalConfig;
import ru.copperside.payadmin.sbp.domain.TkbPayEntry;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.util.List;
import java.util.UUID;

public interface SbpConfigPort {
    List<Upstream> listUpstreams();
    Upstream createUpstream(UpstreamRequest request);
    Upstream patchUpstream(UUID id, UpstreamRequest request);

    List<ExtractionRule> listExtractionRules();
    ExtractionRule createExtractionRule(ExtractionRuleRequest request);
    ExtractionRule patchExtractionRule(UUID id, ExtractionRuleRequest request);

    TerminalConfig getTerminalConfig();
    TerminalConfig putTerminalConfig(TerminalConfigRequest request);

    List<TkbPayEntry> listTkbPay();
    TkbPayEntry addTkbPay(TkbPayEntryRequest request);
    TkbPayEntry removeTkbPay(UUID id);

    List<RoutingFlag> listRoutingFlags();
    RoutingFlag setRoutingFlag(String key, RoutingFlagRequest request);

    PendingChanges pendingChanges();
    void discardDrafts();

    RoutingManifest publishManifest();
    RoutingManifest latestManifest();
    RoutingManifest getManifest(UUID manifestId);
}
