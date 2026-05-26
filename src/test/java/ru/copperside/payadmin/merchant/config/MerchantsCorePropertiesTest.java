package ru.copperside.payadmin.merchant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantsCorePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsDefaults() {
        contextRunner.run(context -> {
            MerchantsCoreProperties props = context.getBean(MerchantsCoreProperties.class);

            assertThat(props.baseUrl()).isEqualTo("http://localhost:8082");
            assertThat(props.internalAdminHeaderName()).isEqualTo("X-Internal-Admin-Key");
            assertThat(props.internalAdminApiKey()).isEmpty();
            assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.pageSize()).isEqualTo(500);
            assertThat(props.maxPages()).isEqualTo(20);
        });
    }

    @Test
    void bindsOverrides() {
        contextRunner
                .withPropertyValues(
                        "payadmin-bff.merchants-core.base-url=http://core.example:8082",
                        "payadmin-bff.merchants-core.internal-admin-header-name=X-Core-Key",
                        "payadmin-bff.merchants-core.internal-admin-api-key=secret",
                        "payadmin-bff.merchants-core.connect-timeout=750ms",
                        "payadmin-bff.merchants-core.read-timeout=3s",
                        "payadmin-bff.merchants-core.page-size=250",
                        "payadmin-bff.merchants-core.max-pages=7"
                )
                .run(context -> {
                    MerchantsCoreProperties props = context.getBean(MerchantsCoreProperties.class);

                    assertThat(props.baseUrl()).isEqualTo("http://core.example:8082");
                    assertThat(props.internalAdminHeaderName()).isEqualTo("X-Core-Key");
                    assertThat(props.internalAdminApiKey()).isEqualTo("secret");
                    assertThat(props.connectTimeout()).isEqualTo(Duration.ofMillis(750));
                    assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(props.pageSize()).isEqualTo(250);
                    assertThat(props.maxPages()).isEqualTo(7);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MerchantsCoreProperties.class)
    static class PropertiesConfiguration {
    }
}

