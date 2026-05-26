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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ListMerchantsUseCaseTest {

    private static final Instant DEFAULT_SINCE = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void mapsCoreMerchantToFrontendShapeUsingActiveSinceForCreatedAt() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(184L, "ООО Ромашка", Map.of("MCC", "5411"), Instant.parse("2025-02-04T10:00:00Z"));

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).hasSize(1);
        assertThat(merchants.getFirst().id()).isEqualTo("MRC-00184");
        assertThat(merchants.getFirst().name()).isEqualTo("ООО Ромашка");
        assertThat(merchants.getFirst().status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchants.getFirst().mcc()).isEqualTo("5411");
        assertThat(merchants.getFirst().createdAt()).isEqualTo(Instant.parse("2025-02-04T10:00:00Z"));
    }

    @Test
    void nullActiveSinceFallsBackToEpoch() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "No Config", Map.of(), null);

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants.getFirst().createdAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void defaultQueryUsesFastPathWithoutPerMerchantCalls() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "A", Map.of("MCC", "5411"), DEFAULT_SINCE)
                .withMerchant(2L, "B", Map.of("MCC", "5412"), DEFAULT_SINCE);

        service(client).list(defaultQuery());

        assertThat(client.activeConfigurationCalls).isZero();
        assertThat(client.activeLineCalls).hasSize(1);
        assertThat(client.activeLineCalls.getFirst().limit()).isEqualTo(100);
        assertThat(client.activeLineCalls.getFirst().sortBy()).isEqualTo("mercId");
    }

    @Test
    void derivesBlockedAndSuspendedStatusesFromConfiguration() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Blocked", Map.of("status", "blocked"), DEFAULT_SINCE)
                .withMerchant(2L, "Suspended", Map.of("STATUS", "suspended"), DEFAULT_SINCE)
                .withMerchant(3L, "Disabled", Map.of("ECOMALLOWED", "0"), DEFAULT_SINCE)
                .withMerchant(4L, "No Config", Map.of(), DEFAULT_SINCE);

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).extracting(AdminMerchant::status).containsExactly(
                MerchantStatus.BLOCKED,
                MerchantStatus.SUSPENDED,
                MerchantStatus.SUSPENDED,
                MerchantStatus.BLOCKED
        );
        assertThat(client.activeConfigurationCalls).isZero();
    }

    @Test
    void resolvesMccCaseInsensitivelyAndFallsBackToUnknownMcc() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "A", Map.of("merchant_mcc", "4111"), DEFAULT_SINCE)
                .withMerchant(2L, "B", Map.of("MERCHANTCATEGORYCODE", "8062"), DEFAULT_SINCE)
                .withMerchant(3L, "C", Map.of("MCC", "bad"), DEFAULT_SINCE)
                .withMerchant(4L, "D", Map.of(), DEFAULT_SINCE);

        List<AdminMerchant> merchants = service(client).list(defaultQuery()).data();

        assertThat(merchants).extracting(AdminMerchant::mcc).containsExactly("4111", "8062", "0000", "0000");
    }

    @Test
    void filtersByStatusViaFullPath() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Active", Map.of("MCC", "5411"), DEFAULT_SINCE)
                .withMerchant(2L, "Suspended", Map.of("status", "suspended"), DEFAULT_SINCE);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(100, 0),
                SearchTerm.empty(),
                MerchantStatus.SUSPENDED,
                SortOrder.of(MerchantSortField.ID, SortDirection.ASC)
        );

        List<AdminMerchant> merchants = service(client).list(query).data();

        assertThat(merchants).extracting(AdminMerchant::id).containsExactly("MRC-00002");
        assertThat(client.activeConfigurationCalls).isZero();
    }

    @Test
    void sortsAndPaginatesViaFullPath() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Gamma", Map.of("MCC", "9999"), DEFAULT_SINCE)
                .withMerchant(2L, "Alpha", Map.of("MCC", "1111"), DEFAULT_SINCE)
                .withMerchant(3L, "Beta", Map.of("MCC", "2222"), DEFAULT_SINCE);

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

    @Test
    void fastPathReportsTotalFromCountCall() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "A", Map.of("MCC", "5411"), DEFAULT_SINCE)
                .withMerchant(2L, "B", Map.of("MCC", "5412"), DEFAULT_SINCE);

        ListMerchantsUseCase.MerchantPage page = service(client).list(defaultQuery());

        assertThat(page.total()).isEqualTo(2L);
        assertThat(client.countCalls).isEqualTo(1);
    }

    @Test
    void fullPathReportsFilteredTotalWithoutCountCall() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .withMerchant(1L, "Active", Map.of("MCC", "5411"), DEFAULT_SINCE)
                .withMerchant(2L, "Suspended", Map.of("status", "suspended"), DEFAULT_SINCE)
                .withMerchant(3L, "AlsoSuspended", Map.of("status", "suspended"), DEFAULT_SINCE);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(1, 0),
                SearchTerm.empty(),
                MerchantStatus.SUSPENDED,
                SortOrder.of(MerchantSortField.ID, SortDirection.ASC)
        );

        ListMerchantsUseCase.MerchantPage page = service(client).list(query);

        assertThat(page.data()).hasSize(1);
        assertThat(page.total()).isEqualTo(2L);
        assertThat(client.countCalls).isZero();
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

    private record Call(int limit, int offset, String sortBy, String sortDir) {
    }

    private static class FakeMerchantCatalogPort implements MerchantCatalogPort {

        private final List<MerchantConfigurationLine> merchants = new ArrayList<>();
        private final List<Call> activeLineCalls = new ArrayList<>();
        private int activeConfigurationCalls = 0;
        private int countCalls = 0;

        FakeMerchantCatalogPort withMerchant(Long mercId, String name, Map<String, String> configuration, Instant activeSince) {
            merchants.add(new MerchantConfigurationLine(mercId, name, configuration, activeSince));
            return this;
        }

        @Override
        public List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir) {
            activeLineCalls.add(new Call(limit, offset, sortBy, sortDir));
            Comparator<MerchantConfigurationLine> byId = Comparator.comparing(MerchantConfigurationLine::mercId);
            if ("desc".equalsIgnoreCase(sortDir)) {
                byId = byId.reversed();
            }
            return merchants.stream().sorted(byId).skip(offset).limit(limit).toList();
        }

        @Override
        public List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId) {
            activeConfigurationCalls++;
            return List.of();
        }

        @Override
        public long countActiveLines(String search) {
            countCalls++;
            return merchants.size();
        }
    }
}
