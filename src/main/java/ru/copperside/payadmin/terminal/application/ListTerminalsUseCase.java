package ru.copperside.payadmin.terminal.application;

import ru.copperside.payadmin.terminal.application.port.out.TerminalCatalogPort;
import ru.copperside.payadmin.terminal.application.port.out.TerminalLine;
import ru.copperside.payadmin.terminal.domain.Terminal;
import ru.copperside.payadmin.terminal.domain.TerminalQuery;
import ru.copperside.payadmin.merchant.domain.SortDirection;

import java.util.List;

public class ListTerminalsUseCase {

    private final TerminalCatalogPort terminalCatalogPort;

    public ListTerminalsUseCase(TerminalCatalogPort terminalCatalogPort) {
        this.terminalCatalogPort = terminalCatalogPort;
    }

    public TerminalPage list(TerminalQuery query) {
        var upstream = terminalCatalogPort.fetchPage(
                query.page().limit(),
                query.page().offset(),
                searchValue(query),
                query.sort().field().value(),
                sortDir(query));
        return toPage(upstream);
    }

    public TerminalPage listByMerchant(long mercId, TerminalQuery query) {
        var upstream = terminalCatalogPort.fetchByMerchant(
                mercId,
                query.page().limit(),
                query.page().offset(),
                searchValue(query),
                query.sort().field().value(),
                sortDir(query));
        return toPage(upstream);
    }

    private TerminalPage toPage(ru.copperside.payadmin.terminal.application.port.out.TerminalPage upstream) {
        List<Terminal> data = upstream.lines().stream().map(this::toTerminal).toList();
        return new TerminalPage(data, data.size(), upstream.total());
    }

    private String searchValue(TerminalQuery query) {
        return (query.search() != null && query.search().isPresent()) ? query.search().value() : null;
    }

    private String sortDir(TerminalQuery query) {
        return query.sort().direction() == SortDirection.DESC ? "desc" : "asc";
    }

    private Terminal toTerminal(TerminalLine line) {
        return new Terminal(
                line.mercId(), line.mps(), line.gate(), line.is3ds(), line.terminalId(),
                line.merchantId(), line.mcc(), line.name(), line.merchantUrl(), line.login(),
                line.hasPassword(), line.apiUrl(), line.merchantName());
    }

    public record TerminalPage(List<Terminal> data, int count, long total) {
    }
}
