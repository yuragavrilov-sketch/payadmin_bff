package ru.copperside.payadmin.merchant.application.port.out;

import java.util.List;

public interface MerchantCatalogPort {

    List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir);

    List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId);

    long countActiveLines(String search);
}

