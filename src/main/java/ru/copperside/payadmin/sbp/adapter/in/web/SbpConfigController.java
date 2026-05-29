package ru.copperside.payadmin.sbp.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.copperside.payadmin.common.web.ApiResponse;
import ru.copperside.payadmin.sbp.application.config.ExtractionRuleRequest;
import ru.copperside.payadmin.sbp.application.config.RoutingFlagRequest;
import ru.copperside.payadmin.sbp.application.config.SbpConfigUseCase;
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

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sbp")
public class SbpConfigController {

    private final SbpConfigUseCase useCase;
    private final Clock clock;

    public SbpConfigController(SbpConfigUseCase useCase, Clock clock) {
        this.useCase = useCase;
        this.clock = clock;
    }

    @GetMapping("/upstreams")
    public ApiResponse<List<Upstream>> listUpstreams() {
        return ApiResponse.success(useCase.listUpstreams(), clock);
    }

    @PostMapping("/upstreams")
    public ApiResponse<Upstream> createUpstream(@RequestBody UpstreamRequest request) {
        return ApiResponse.success(useCase.createUpstream(request), clock);
    }

    @PatchMapping("/upstreams/{id}")
    public ApiResponse<Upstream> patchUpstream(@PathVariable UUID id, @RequestBody UpstreamRequest request) {
        return ApiResponse.success(useCase.patchUpstream(id, request), clock);
    }

    @DeleteMapping("/upstreams/{id}")
    public ApiResponse<Upstream> removeUpstream(@PathVariable UUID id) {
        return ApiResponse.success(useCase.removeUpstream(id), clock);
    }

    @GetMapping("/extraction-rules")
    public ApiResponse<List<ExtractionRule>> listExtractionRules() {
        return ApiResponse.success(useCase.listExtractionRules(), clock);
    }

    @PostMapping("/extraction-rules")
    public ApiResponse<ExtractionRule> createExtractionRule(@RequestBody ExtractionRuleRequest request) {
        return ApiResponse.success(useCase.createExtractionRule(request), clock);
    }

    @PatchMapping("/extraction-rules/{id}")
    public ApiResponse<ExtractionRule> patchExtractionRule(
            @PathVariable UUID id, @RequestBody ExtractionRuleRequest request) {
        return ApiResponse.success(useCase.patchExtractionRule(id, request), clock);
    }

    @DeleteMapping("/extraction-rules/{id}")
    public ApiResponse<ExtractionRule> removeExtractionRule(@PathVariable UUID id) {
        return ApiResponse.success(useCase.removeExtractionRule(id), clock);
    }

    @GetMapping("/terminal-config")
    public ApiResponse<TerminalConfig> getTerminalConfig() {
        return ApiResponse.success(useCase.getTerminalConfig(), clock);
    }

    @PutMapping("/terminal-config")
    public ApiResponse<TerminalConfig> putTerminalConfig(@RequestBody TerminalConfigRequest request) {
        return ApiResponse.success(useCase.putTerminalConfig(request), clock);
    }

    @GetMapping("/tkb-pay-list")
    public ApiResponse<List<TkbPayEntry>> listTkbPay() {
        return ApiResponse.success(useCase.listTkbPay(), clock);
    }

    @PostMapping("/tkb-pay-list")
    public ApiResponse<TkbPayEntry> addTkbPay(@RequestBody TkbPayEntryRequest request) {
        return ApiResponse.success(useCase.addTkbPay(request), clock);
    }

    @DeleteMapping("/tkb-pay-list/{id}")
    public ApiResponse<TkbPayEntry> removeTkbPay(@PathVariable UUID id) {
        return ApiResponse.success(useCase.removeTkbPay(id), clock);
    }

    @GetMapping("/routing-flags")
    public ApiResponse<List<RoutingFlag>> listRoutingFlags() {
        return ApiResponse.success(useCase.listRoutingFlags(), clock);
    }

    @PutMapping("/routing-flags/{key}")
    public ApiResponse<RoutingFlag> setRoutingFlag(
            @PathVariable String key, @RequestBody RoutingFlagRequest request) {
        return ApiResponse.success(useCase.setRoutingFlag(key, request), clock);
    }

    @GetMapping("/pending-changes")
    public ApiResponse<PendingChanges> pendingChanges() {
        return ApiResponse.success(useCase.pendingChanges(), clock);
    }

    @DeleteMapping("/drafts")
    public ApiResponse<Map<String, String>> discardDrafts() {
        useCase.discardDrafts();
        return ApiResponse.success(Map.of("status", "discarded"), clock);
    }

    @PostMapping("/routing-manifests")
    public ApiResponse<RoutingManifest> publishManifest() {
        return ApiResponse.success(useCase.publishManifest(), clock);
    }

    @GetMapping("/routing-manifests/latest")
    public ApiResponse<RoutingManifest> latestManifest() {
        return ApiResponse.success(useCase.latestManifest(), clock);
    }

    @GetMapping("/routing-manifests/{manifestId}")
    public ApiResponse<RoutingManifest> getManifest(@PathVariable UUID manifestId) {
        return ApiResponse.success(useCase.getManifest(manifestId), clock);
    }
}
