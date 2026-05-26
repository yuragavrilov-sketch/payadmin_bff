package ru.copperside.payadmin.merchant.application;

import org.junit.jupiter.api.Test;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationEntry;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;
import ru.copperside.payadmin.merchant.config.PayadminMerchantsProperties;
import ru.copperside.payadmin.merchant.domain.AdminMerchant;
import ru.copperside.payadmin.merchant.domain.MerchantQuery;
import ru.copperside.payadmin.merchant.domain.MerchantSortField;
import ru.copperside.payadmin.merchant.domain.MerchantStatus;
import ru.copperside.payadmin.merchant.domain.PageWindow;
import ru.copperside.payadmin.merchant.domain.SearchTerm;
import ru.copperside.payadmin.merchant.domain.SortDirection;
import ru.copperside.payadmin.merchant.domain.SortOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListMerchantsUseCaseTest {

    @Test
    void mapsCoreMerchantToFrontendShape() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(184L, "ООО Ромашка", Map.of("MCC", "5411"))
                .withConfig(184L,
                        entry("NAME", "ООО Ромашка", "2025-02-04T10:00:00Z"),
                        entry("MCC", "5411", "2025-02-05T10:00:00Z")
                );

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).hasSize(1);
        assertThat(merchants.getFirst().id()).isEqualTo("MRC-00184");
        assertThat(merchants.getFirst().name()).isEqualTo("ООО Ромашка");
        assertThat(merchants.getFirst().status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchants.getFirst().mcc()).isEqualTo("5411");
        assertThat(merchants.getFirst().createdAt()).isEqualTo(Instant.parse("2025-02-04T10:00:00Z"));
    }

    @Test
    void derivesBlockedAndSuspendedStatusesFromConfiguration() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Blocked", Map.of("status", "blocked"))
                .withMerchant(2L, "Suspended", Map.of("STATUS", "suspended"))
                .withMerchant(3L, "Disabled", Map.of("ECOMALLOWED", "0"))
                .withMerchant(4L, "No Config", Map.of());
        client.withDefaultConfigFor(1L, 2L, 3L);

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).extracting(AdminMerchant::status).containsExactly(
                MerchantStatus.BLOCKED,
                MerchantStatus.SUSPENDED,
                MerchantStatus.SUSPENDED,
                MerchantStatus.BLOCKED
        );
    }

    @Test
    void resolvesMccCaseInsensitivelyAndFallsBackToUnknownMcc() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "A", Map.of("merchant_mcc", "4111"))
                .withMerchant(2L, "B", Map.of("MERCHANTCATEGORYCODE", "8062"))
                .withMerchant(3L, "C", Map.of("MCC", "bad"))
                .withMerchant(4L, "D", Map.of());
        client.withDefaultConfigFor(1L, 2L, 3L, 4L);

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).extracting(AdminMerchant::mcc).containsExactly("4111", "8062", "0000", "0000");
    }

    @Test
    void filtersByStatusAfterMapping() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Active", Map.of("MCC", "5411"))
                .withMerchant(2L, "Suspended", Map.of("status", "suspended"));
        client.withDefaultConfigFor(1L, 2L);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(100, 0),
                SearchTerm.empty(),
                MerchantStatus.SUSPENDED,
                SortOrder.of(MerchantSortField.ID, SortDirection.ASC)
        );

        List<AdminMerchant> merchants = service(client).list(query).data();

        assertThat(merchants).extracting(AdminMerchant::id).containsExactly("MRC-00002");
    }

    @Test
    void sortsAndPaginatesMappedMerchants() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Gamma", Map.of("MCC", "9999"))
                .withMerchant(2L, "Alpha", Map.of("MCC", "1111"))
                .withMerchant(3L, "Beta", Map.of("MCC", "2222"));
        client.withDefaultConfigFor(1L, 2L, 3L);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(2, 1),
                SearchTerm.empty(),
                null,
                SortOrder.of(MerchantSortField.NAME, SortDirection.ASC)
        );

        ListMerchantsUseCase.MerchantPage page = service(client).list(query);

        assertThat(page.data()).extracting(AdminMerchant::name).containsExactly("Beta", "Gamma");
        assertThat(page.count()).isEqualTo(2);
    }

    private MerchantQuery defaultQuery() {
        return new MerchantQuery(
                PageWindow.of(100, 0),
                SearchTerm.empty(),
                null,
                SortOrder.of(MerchantSortField.ID, SortDirection.ASC)
        );
    }

    private ListMerchantsUseCase service(FakeMerchantCatalogPort client) {
        return new ListMerchantsUseCase(client, new PayadminMerchantsProperties("0000"));
    }

    private static MerchantConfigurationEntry entry(String name, String value, String dateBegin) {
        return new MerchantConfigurationEntry(
                name,
                value,
                Instant.parse(dateBegin),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static class FakeMerchantCatalogPort implements MerchantCatalogPort {

        private final List<MerchantConfigurationLine> merchants = new ArrayList<>();
        private final Map<Long, List<MerchantConfigurationEntry>> configByMerchant = new LinkedHashMap<>();

        FakeMerchantCatalogPort withMerchant(Long mercId, String name, Map<String, String> configuration) {
            merchants.add(new MerchantConfigurationLine(mercId, name, configuration));
            return this;
        }

        void withDefaultConfigFor(Long... merchantIds) {
            for (Long merchantId : merchantIds) {
                withConfig(merchantId, entry("NAME", "Merchant " + merchantId, "2024-01-01T00:00:00Z"));
            }
        }

        FakeMerchantCatalogPort withConfig(Long merchantId, MerchantConfigurationEntry... entries) {
            configByMerchant.put(merchantId, List.of(entries));
            return this;
        }

        @Override
        public List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir) {
            return merchants.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId) {
            return configByMerchant.getOrDefault(merchantId, List.of());
        }
    }
}


