package ru.copperside.payadmin.merchant.application;

import ru.copperside.payadmin.merchant.config.MerchantsCoreProperties;
import ru.copperside.payadmin.merchant.config.PayadminMerchantsProperties;
import ru.copperside.payadmin.merchant.domain.AdminMerchant;
import ru.copperside.payadmin.merchant.domain.MerchantQuery;
import ru.copperside.payadmin.merchant.domain.MerchantSortField;
import ru.copperside.payadmin.merchant.domain.MerchantStatus;
import ru.copperside.payadmin.merchant.domain.SortDirection;
import ru.copperside.payadmin.merchant.application.port.out.MerchantCatalogPort;
import ru.copperside.payadmin.merchant.application.port.out.MerchantConfigurationLine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ListMerchantsUseCase {

    private static final Set<String> MCC_KEYS = Set.of("mcc", "merchant_mcc", "merchantcategorycode");
    private static final Set<String> DISABLE_FLAGS = Set.of(
            "ecomallowed",
            "directecomallow",
            "onlineecomenabled",
            "onlinep2penabled",
            "p2pdebitallowed",
            "p2pcreditallowed"
    );

    private final MerchantCatalogPort merchantCatalogPort;
    private final PayadminMerchantsProperties merchantsProperties;
    private final int upstreamPageSize;
    private final int maxPages;

    public ListMerchantsUseCase(
            MerchantCatalogPort merchantCatalogPort,
            PayadminMerchantsProperties merchantsProperties,
            MerchantsCoreProperties merchantsCoreProperties
    ) {
        this(
                merchantCatalogPort,
                merchantsProperties,
                merchantsCoreProperties.pageSize(),
                merchantsCoreProperties.maxPages()
        );
    }

    ListMerchantsUseCase(MerchantCatalogPort merchantCatalogPort, PayadminMerchantsProperties merchantsProperties) {
        this(merchantCatalogPort, merchantsProperties, 500, 20);
    }

    private ListMerchantsUseCase(
            MerchantCatalogPort merchantCatalogPort,
            PayadminMerchantsProperties merchantsProperties,
            int upstreamPageSize,
            int maxPages
    ) {
        this.merchantCatalogPort = merchantCatalogPort;
        this.merchantsProperties = merchantsProperties;
        this.upstreamPageSize = upstreamPageSize;
        this.maxPages = maxPages;
    }

    public MerchantPage list(MerchantQuery query) {
        if (canPushDown(query)) {
            List<AdminMerchant> page = fetchRequestedPage(query).stream()
                    .map(this::toAdminMerchant)
                    .toList();
            long total = merchantCatalogPort.countActiveLines(
                    query.search().isPresent() ? query.search().value() : null);
            return new MerchantPage(page, page.size(), total);
        }

        List<AdminMerchant> mapped = fetchAllActiveLines().stream()
                .map(this::toAdminMerchant)
                .filter(merchant -> matchesSearch(merchant, query))
                .filter(merchant -> query.status() == null || merchant.status() == query.status())
                .sorted(comparator(query))
                .toList();

        List<AdminMerchant> page = mapped.stream()
                .skip(query.page().offset())
                .limit(query.page().limit())
                .toList();

        return new MerchantPage(page, page.size(), mapped.size());
    }

    private boolean canPushDown(MerchantQuery query) {
        boolean noSearch = query.search() == null || !query.search().isPresent();
        return noSearch
                && query.status() == null
                && query.sort().field() == MerchantSortField.ID;
    }

    private List<MerchantConfigurationLine> fetchRequestedPage(MerchantQuery query) {
        String sortDir = query.sort().direction() == SortDirection.DESC ? "desc" : "asc";
        return merchantCatalogPort.fetchActiveLines(
                query.page().limit(),
                query.page().offset(),
                null,
                "mercId",
                sortDir
        );
    }

    private List<MerchantConfigurationLine> fetchAllActiveLines() {
        List<MerchantConfigurationLine> result = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            int offset = page * upstreamPageSize;
            List<MerchantConfigurationLine> chunk = merchantCatalogPort.fetchActiveLines(
                    upstreamPageSize,
                    offset,
                    null,
                    "mercId",
                    "asc"
            );
            result.addAll(chunk);
            if (chunk.size() < upstreamPageSize) {
                break;
            }
        }
        return result;
    }

    private AdminMerchant toAdminMerchant(MerchantConfigurationLine merchant) {
        Instant createdAt = merchant.activeSince() == null ? Instant.EPOCH : merchant.activeSince();
        return new AdminMerchant(
                formatMerchantId(merchant.mercId()),
                merchant.name(),
                deriveStatus(merchant.configuration()),
                resolveMcc(merchant.configuration()),
                createdAt
        );
    }

    private String formatMerchantId(Long mercId) {
        return "MRC-" + String.format(Locale.ROOT, "%05d", mercId);
    }

    private MerchantStatus deriveStatus(Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return MerchantStatus.BLOCKED;
        }

        Optional<String> explicitStatus = valueFor(configuration, "status").map(value -> value.toLowerCase(Locale.ROOT));
        if (explicitStatus.isPresent()) {
            if ("blocked".equals(explicitStatus.get())) {
                return MerchantStatus.BLOCKED;
            }
            if ("suspended".equals(explicitStatus.get())) {
                return MerchantStatus.SUSPENDED;
            }
        }

        boolean disabled = configuration.entrySet().stream()
                .filter(entry -> DISABLE_FLAGS.contains(normalizeKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .anyMatch(this::isDisabledValue);
        if (disabled) {
            return MerchantStatus.SUSPENDED;
        }

        return MerchantStatus.ACTIVE;
    }

    private String resolveMcc(Map<String, String> configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return merchantsProperties.unknownMcc();
        }

        return configuration.entrySet().stream()
                .filter(entry -> MCC_KEYS.contains(normalizeKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && value.matches("\\d{4}"))
                .findFirst()
                .orElse(merchantsProperties.unknownMcc());
    }

    private Optional<String> valueFor(Map<String, String> configuration, String key) {
        String normalizedKey = normalizeKey(key);
        return configuration.entrySet().stream()
                .filter(entry -> normalizedKey.equals(normalizeKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private boolean isDisabledValue(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("0")
                || normalized.equals("false")
                || normalized.equals("no")
                || normalized.equals("n")
                || normalized.equals("disabled");
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(AdminMerchant merchant, MerchantQuery query) {
        if (query.search() == null || !query.search().isPresent()) {
            return true;
        }
        String term = query.search().value();
        return merchant.id().toLowerCase(Locale.ROOT).contains(term)
                || merchant.name().toLowerCase(Locale.ROOT).contains(term)
                || merchant.status().value().contains(term)
                || merchant.mcc().contains(term)
                || merchant.createdAt().toString().toLowerCase(Locale.ROOT).contains(term);
    }

    private Comparator<AdminMerchant> comparator(MerchantQuery query) {
        MerchantSortField field = query.sort().field();
        Comparator<AdminMerchant> comparator = switch (field) {
            case ID -> Comparator.comparing(AdminMerchant::id);
            case NAME -> Comparator.comparing(AdminMerchant::name, String.CASE_INSENSITIVE_ORDER);
            case STATUS -> Comparator.comparing(merchant -> merchant.status().value());
            case MCC -> Comparator.comparing(AdminMerchant::mcc);
            case CREATED_AT -> Comparator.comparing(AdminMerchant::createdAt);
        };
        if (query.sort().direction() == SortDirection.DESC) {
            return comparator.reversed();
        }
        return comparator;
    }

    public record MerchantPage(List<AdminMerchant> data, int count, long total) {
    }
}

