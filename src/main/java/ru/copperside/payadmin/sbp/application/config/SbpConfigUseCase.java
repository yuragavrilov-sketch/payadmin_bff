package ru.copperside.payadmin.sbp.application.config;

import ru.copperside.payadmin.sbp.application.config.port.out.SbpConfigPort;
import ru.copperside.payadmin.sbp.domain.ExtractionRule;
import ru.copperside.payadmin.sbp.domain.PendingChanges;
import ru.copperside.payadmin.sbp.domain.RoutingFlag;
import ru.copperside.payadmin.sbp.domain.RoutingManifest;
import ru.copperside.payadmin.sbp.domain.TerminalConfig;
import ru.copperside.payadmin.sbp.domain.TkbPayEntry;
import ru.copperside.payadmin.sbp.domain.Upstream;

import java.util.List;
import java.util.UUID;

public class SbpConfigUseCase {

    private final SbpConfigPort port;

    public SbpConfigUseCase(SbpConfigPort port) {
        this.port = port;
    }

    public List<Upstream> listUpstreams() { return port.listUpstreams(); }
    public Upstream createUpstream(UpstreamRequest r) { return port.createUpstream(r); }
    public Upstream patchUpstream(UUID id, UpstreamRequest r) { return port.patchUpstream(id, r); }

    public List<ExtractionRule> listExtractionRules() { return port.listExtractionRules(); }
    public ExtractionRule createExtractionRule(ExtractionRuleRequest r) { return port.createExtractionRule(r); }
    public ExtractionRule patchExtractionRule(UUID id, ExtractionRuleRequest r) { return port.patchExtractionRule(id, r); }

    public TerminalConfig getTerminalConfig() { return port.getTerminalConfig(); }
    public TerminalConfig putTerminalConfig(TerminalConfigRequest r) { return port.putTerminalConfig(r); }

    public List<TkbPayEntry> listTkbPay() { return port.listTkbPay(); }
    public TkbPayEntry addTkbPay(TkbPayEntryRequest r) { return port.addTkbPay(r); }
    public TkbPayEntry removeTkbPay(UUID id) { return port.removeTkbPay(id); }

    public List<RoutingFlag> listRoutingFlags() { return port.listRoutingFlags(); }
    public RoutingFlag setRoutingFlag(String key, RoutingFlagRequest r) { return port.setRoutingFlag(key, r); }

    public PendingChanges pendingChanges() { return port.pendingChanges(); }
    public void discardDrafts() { port.discardDrafts(); }

    public RoutingManifest publishManifest() { return port.publishManifest(); }
    public RoutingManifest latestManifest() { return port.latestManifest(); }
    public RoutingManifest getManifest(UUID id) { return port.getManifest(id); }
}
