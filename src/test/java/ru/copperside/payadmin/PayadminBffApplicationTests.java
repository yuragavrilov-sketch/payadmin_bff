package ru.copperside.payadmin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class PayadminBffApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsWithLocalProfile() {
    }

    @Test
    void localProfileUsesStandardConfigServerLabelAndVaultKvDefaults() {
        assertThat(environment.getProperty("pay.environment")).isEqualTo("local");
        assertThat(environment.getProperty("spring.cloud.config.label")).isEqualTo("local");
        assertThat(environment.getProperty("spring.cloud.vault.kv.backend")).isEqualTo("pay");
        assertThat(environment.getProperty("spring.cloud.vault.kv.application-name"))
                .isEqualTo("local/payadmin-bff-merchants-core-internal-admin-key");
    }
}

