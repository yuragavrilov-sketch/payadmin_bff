package ru.copperside.payadmin.merchant.application;

import org.junit.jupiter.api.Test;
import ru.copperside.payadmin.merchant.application.port.out.MerchantAdminLine;
import ru.copperside.payadmin.merchant.application.port.out.MerchantAdminPage;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationEntry;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListMerchantsUseCaseTest {

    @Test
    void mapsAdminLineToFrontendShape() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .returning(7L, new MerchantAdminLine(184L, "ООО Ромашка", "active", "5411",
                        "7712345678", Instant.parse("2025-02-04T10:00:00Z")));

        ListMerchantsUseCase.MerchantPage page = service(client).list(defaultQuery());

        assertThat(page.total()).isEqualTo(7L);
        assertThat(page.count()).isEqualTo(1);
        AdminMerchant m = page.data().getFirst();
        assertThat(m.id()).isEqualTo("MRC-00184");
        assertThat(m.name()).isEqualTo("ООО Ромашка");
        assertThat(m.status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(m.mcc()).isEqualTo("5411");
        assertThat(m.inn()).isEqualTo("7712345678");
        assertThat(m.createdAt()).isEqualTo(Instant.parse("2025-02-04T10:00:00Z"));
    }

    @Test
    void nullCreatedAtFallsBackToEpoch() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .returning(1L, new MerchantAdminLine(1L, "No Config", "blocked", "0000", null, null));

        AdminMerchant m = service(client).list(defaultQuery()).data().getFirst();

        assertThat(m.createdAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void forwardsQueryParamsMappingIdToMercId() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort().returning(0L);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(20, 40),
                SearchTerm.of("shop"),
                MerchantStatus.SUSPENDED,
                SortOrder.of(MerchantSortField.ID, SortDirection.DESC));

        service(client).list(query);

        assertThat(client.lastLimit).isEqualTo(20);
        assertThat(client.lastOffset).isEqualTo(40);
        assertThat(client.lastSearch).isEqualTo("shop");
        assertThat(client.lastStatus).isEqualTo("suspended");
        assertThat(client.lastSortBy).isEqualTo("mercId");
        assertThat(client.lastSortDir).isEqualTo("desc");
    }

    @Test
    void forwardsNonIdSortFieldVerbatimAndOmitsEmptySearchStatus() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort().returning(0L);

        MerchantQuery query = new MerchantQuery(
                PageWindow.of(50, 0),
                SearchTerm.empty(),
                null,
                SortOrder.of(MerchantSortField.NAME, SortDirection.ASC));

        service(client).list(query);

        assertThat(client.lastSortBy).isEqualTo("name");
        assertThat(client.lastSearch).isNull();
        assertThat(client.lastStatus).isNull();
        assertThat(client.lastSortDir).isEqualTo("asc");
    }

    @Test
    void unknownOrNullUpstreamStatusDefaultsToBlocked() {
        FakeMerchantCatalogPort client = new FakeMerchantCatalogPort()
                .returning(2L,
                        new MerchantAdminLine(1L, "Null Status", null, "5411", null, null),
                        new MerchantAdminLine(2L, "Bogus Status", "pending", "5411", null, null));

        List<AdminMerchant> data = service(client).list(defaultQuery()).data();

        assertThat(data).extracting(AdminMerchant::status)
                .containsExactly(MerchantStatus.BLOCKED, MerchantStatus.BLOCKED);
    }

    private MerchantQuery defaultQuery() {
        return new MerchantQuery(
                PageWindow.of(100, 0),
                SearchTerm.empty(),
                null,
                SortOrder.of(MerchantSortField.ID, SortDirection.ASC));
    }

    private ListMerchantsUseCase service(FakeMerchantCatalogPort client) {
        return new ListMerchantsUseCase(client);
    }

    private static class FakeMerchantCatalogPort implements MerchantCatalogPort {
        private final List<MerchantAdminLine> lines = new ArrayList<>();
        private long total;
        Integer lastLimit;
        Integer lastOffset;
        String lastSearch;
        String lastStatus;
        String lastSortBy;
        String lastSortDir;

        FakeMerchantCatalogPort returning(long total, MerchantAdminLine... rows) {
            this.total = total;
            this.lines.addAll(List.of(rows));
            return this;
        }

        @Override
        public MerchantAdminPage fetchAdminPage(int limit, int offset, String search, String status, String sortBy, String sortDir) {
            lastLimit = limit;
            lastOffset = offset;
            lastSearch = search;
            lastStatus = status;
            lastSortBy = sortBy;
            lastSortDir = sortDir;
            return new MerchantAdminPage(lines, total);
        }

        @Override
        public List<MerchantConfigurationLine> fetchActiveLines(int limit, int offset, String search, String sortBy, String sortDir) {
            return List.of();
        }

        @Override
        public long countActiveLines(String search) {
            return 0L;
        }

        @Override
        public List<MerchantConfigurationEntry> fetchActiveConfiguration(Long merchantId) {
            return List.of();
        }
    }
}
