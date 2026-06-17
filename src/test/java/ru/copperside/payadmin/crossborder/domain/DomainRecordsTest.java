package ru.copperside.payadmin.crossborder.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class DomainRecordsTest {
    @Test
    void recordsHoldValues() {
        PartnerMethod m = new PartnerMethod("card", 16774L, "USD", List.of("AZN"), List.of());
        PartnerProvider p = new PartnerProvider("all", "Все банки", "All banks", List.of(m));
        PartnerCountry c = new PartnerCountry("AZ", "Азербайджан", "Azerbaijan", List.of(p));
        assertThat(c.providers().get(0).methods().get(0).walletId()).isEqualTo(16774L);
        assertThat(c.providers().get(0).methods().get(0).supportedCurrencies()).containsExactly("AZN");
    }
}
