package ru.copperside.payadmin.terminal.domain;

import ru.copperside.payadmin.merchant.domain.PageWindow;
import ru.copperside.payadmin.merchant.domain.SearchTerm;
import ru.copperside.payadmin.merchant.domain.SortOrder;

public record TerminalQuery(
        PageWindow page,
        SearchTerm search,
        SortOrder<TerminalSortField> sort
) {
}
