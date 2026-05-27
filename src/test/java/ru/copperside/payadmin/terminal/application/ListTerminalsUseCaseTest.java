package ru.copperside.payadmin.terminal.application;

import org.junit.jupiter.api.Test;
import ru.copperside.payadmin.terminal.application.port.out.TerminalCatalogPort;
import ru.copperside.payadmin.terminal.application.port.out.TerminalLine;
import ru.copperside.payadmin.terminal.application.port.out.TerminalPage;
import ru.copperside.payadmin.terminal.domain.Terminal;
import ru.copperside.payadmin.terminal.domain.TerminalQuery;
import ru.copperside.payadmin.terminal.domain.TerminalSortField;
import ru.copperside.payadmin.merchant.domain.PageWindow;
import ru.copperside.payadmin.merchant.domain.SearchTerm;
import ru.copperside.payadmin.merchant.domain.SortDirection;
import ru.copperside.payadmin.merchant.domain.SortOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListTerminalsUseCaseTest {

    @Test
    void mapsLineToDomainTerminal() {
        FakePort port = new FakePort().returning(3L,
                new TerminalLine(1L, "VISA", "ECOM", true, "T1", "M1", "5411", "Alpha VISA",
                        "http://a", "login1", true, "http://api1", "Alpha Shop"));

        ListTerminalsUseCase.TerminalPage page = new ListTerminalsUseCase(port).list(query());

        assertThat(page.total()).isEqualTo(3L);
        assertThat(page.count()).isEqualTo(1);
        Terminal t = page.data().getFirst();
        assertThat(t.mercId()).isEqualTo(1L);
        assertThat(t.mps()).isEqualTo("VISA");
        assertThat(t.is3ds()).isTrue();
        assertThat(t.hasPassword()).isTrue();
        assertThat(t.merchantName()).isEqualTo("Alpha Shop");
    }

    @Test
    void listForwardsQueryParams() {
        FakePort port = new FakePort().returning(0L);

        new ListTerminalsUseCase(port).list(new TerminalQuery(
                PageWindow.of(20, 40),
                SearchTerm.of("visa"),
                SortOrder.of(TerminalSortField.MPS, SortDirection.DESC)));

        assertThat(port.lastLimit).isEqualTo(20);
        assertThat(port.lastOffset).isEqualTo(40);
        assertThat(port.lastSearch).isEqualTo("visa");
        assertThat(port.lastSortBy).isEqualTo("mps");
        assertThat(port.lastSortDir).isEqualTo("desc");
        assertThat(port.lastMercId).isNull();
    }

    @Test
    void listByMerchantForwardsMercIdAndOmitsEmptySearch() {
        FakePort port = new FakePort().returning(0L);

        new ListTerminalsUseCase(port).listByMerchant(7L, new TerminalQuery(
                PageWindow.of(50, 0),
                SearchTerm.empty(),
                SortOrder.of(TerminalSortField.MERC_ID, SortDirection.ASC)));

        assertThat(port.lastMercId).isEqualTo(7L);
        assertThat(port.lastSearch).isNull();
        assertThat(port.lastSortBy).isEqualTo("mercId");
    }

    private TerminalQuery query() {
        return new TerminalQuery(PageWindow.of(100, 0), SearchTerm.empty(),
                SortOrder.of(TerminalSortField.MERC_ID, SortDirection.ASC));
    }

    private static class FakePort implements TerminalCatalogPort {
        private final List<TerminalLine> lines = new java.util.ArrayList<>();
        private long total;
        Integer lastLimit;
        Integer lastOffset;
        String lastSearch;
        String lastSortBy;
        String lastSortDir;
        Long lastMercId;

        FakePort returning(long total, TerminalLine... rows) {
            this.total = total;
            this.lines.addAll(List.of(rows));
            return this;
        }

        @Override
        public TerminalPage fetchPage(int limit, int offset, String search, String sortBy, String sortDir) {
            lastLimit = limit; lastOffset = offset; lastSearch = search;
            lastSortBy = sortBy; lastSortDir = sortDir; lastMercId = null;
            return new TerminalPage(lines, total);
        }

        @Override
        public TerminalPage fetchByMerchant(long mercId, int limit, int offset, String search, String sortBy, String sortDir) {
            lastMercId = mercId; lastLimit = limit; lastOffset = offset; lastSearch = search;
            lastSortBy = sortBy; lastSortDir = sortDir;
            return new TerminalPage(lines, total);
        }
    }
}
