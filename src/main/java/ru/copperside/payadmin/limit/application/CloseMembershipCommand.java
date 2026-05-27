package ru.copperside.payadmin.limit.application;

import java.time.Instant;

public record CloseMembershipCommand(Instant validTo) {
}
