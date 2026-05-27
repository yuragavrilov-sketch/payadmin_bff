package ru.copperside.payadmin.terminal.application.port.out;

import java.util.List;

public record TerminalPage(List<TerminalLine> lines, long total) {
}
