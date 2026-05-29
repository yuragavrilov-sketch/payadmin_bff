package ru.copperside.payadmin.sbp.domain;

import java.util.UUID;

public record TkbPayEntry(UUID id, String rcvTspId, String status, boolean removal) {
}
