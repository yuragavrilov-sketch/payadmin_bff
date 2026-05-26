package ru.copperside.payadmin.merchant.application.port.out;

import java.util.List;

public record MerchantAdminPage(List<MerchantAdminLine> lines, long total) {
}
