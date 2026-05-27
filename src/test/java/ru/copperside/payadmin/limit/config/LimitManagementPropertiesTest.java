package ru.copperside.payadmin.limit.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LimitManagementPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsDefaults() {
        contextRunner.run(context -> {
            LimitManagementProperties props = context.getBean(LimitManagementProperties.class);

            assertThat(props.baseUrl()).isEqualTo("http://localhost:8084");
            assertThat(props.internalAdminHeaderName()).isEqualTo("X-Internal-Admin-Key");
            assertThat(props.internalAdminApiKey()).isEmpty();
            assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    @Test
    void bindsOverrides() {
        contextRunner
                .withPropertyValues(
                        "payadmin-bff.limit-management.base-url=http://localhost:8084",
                        "payadmin-bff.limit-management.internal-admin-header-name=X-Limit-Key",
                        "payadmin-bff.limit-management.internal-admin-api-key=secret",
                        "payadmin-bff.limit-management.connect-timeout=1s",
                        "payadmin-bff.limit-management.read-timeout=3s"
                )
                .run(context -> {
                    LimitManagementProperties props = context.getBean(LimitManagementProperties.class);

                    assertThat(props.baseUrl()).isEqualTo("http://localhost:8084");
                    assertThat(props.internalAdminHeaderName()).isEqualTo("X-Limit-Key");
                    assertThat(props.internalAdminApiKey()).isEqualTo("secret");
                    assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(3));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LimitManagementProperties.class)
    static class PropertiesConfiguration {
    }
}
