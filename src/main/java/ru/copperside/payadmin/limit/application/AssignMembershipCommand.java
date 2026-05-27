package ru.copperside.payadmin.limit.application;

import java.time.Instant;
import java.util.UUID;

public record AssignMembershipCommand(String merchantId, UUID groupId, Instant validFrom) {
}
