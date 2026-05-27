package ru.copperside.payadmin.merchant.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.copperside.payadmin.merchant.application.port.out.MerchantAdminLine;
import ru.copperside.payadmin.merchant.application.port.out.MerchantAdminPage;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;
import ru.copperside.payadmin.merchant.domain.AdminMerchant;
import ru.copperside.payadmin.merchant.domain.MerchantQuery;
import ru.copperside.payadmin.merchant.domain.MerchantSortField;
import ru.copperside.payadmin.merchant.domain.MerchantStatus;
import ru.copperside.payadmin.merchant.domain.SortDirection;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public class ListMerchantsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListMerchantsUseCase.class);

    private final MerchantCatalogPort merchantCatalogPort;

    public ListMerchantsUseCase(MerchantCatalogPort merchantCatalogPort) {
        this.merchantCatalogPort = merchantCatalogPort;
    }

    public MerchantPage list(MerchantQuery query) {
        String sortBy = query.sort().field() == MerchantSortField.ID
                ? "mercId"
                : query.sort().field().value();
        String sortDir = query.sort().direction() == SortDirection.DESC ? "desc" : "asc";
        String search = (query.search() != null && query.search().isPresent())
                ? query.search().value()
                : null;
        String status = query.status() == null ? null : query.status().value();

        MerchantAdminPage upstream = merchantCatalogPort.fetchAdminPage(
                query.page().limit(),
                query.page().offset(),
                search,
                status,
                sortBy,
                sortDir
        );

        List<AdminMerchant> page = upstream.lines().stream()
                .map(this::toAdminMerchant)
                .toList();

        return new MerchantPage(page, page.size(), upstream.total());
    }

    private AdminMerchant toAdminMerchant(MerchantAdminLine line) {
        Instant createdAt = line.createdAt() == null ? Instant.EPOCH : line.createdAt();
        return new AdminMerchant(
                formatMerchantId(line.mercId()),
                line.name(),
                parseStatus(line.status(), line.mercId()),
                line.mcc(),
                line.inn(),
                createdAt
        );
    }

    private MerchantStatus parseStatus(String status, Long mercId) {
        try {
            return MerchantStatus.from(status);
        } catch (IllegalArgumentException ex) {
            log.warn("merchants-core returned unrecognised status '{}' for merchant {}; defaulting to BLOCKED", status, mercId);
            return MerchantStatus.BLOCKED;
        }
    }

    private String formatMerchantId(Long mercId) {
        return "MRC-" + String.format(Locale.ROOT, "%05d", mercId);
    }

    public record MerchantPage(List<AdminMerchant> data, int count, long total) {
    }
}
