package ru.copperside.payadmin.limit.application;

import java.util.UUID;

public record MembershipQuery(String merchantId, UUID typeId, UUID groupId, String state) {
}
