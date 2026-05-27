package ru.copperside.payadmin.terminal.application.port.out;

public interface TerminalCatalogPort {

    TerminalPage fetchPage(int limit, int offset, String search, String sortBy, String sortDir);

    TerminalPage fetchByMerchant(long mercId, int limit, int offset, String search, String sortBy, String sortDir);
}
