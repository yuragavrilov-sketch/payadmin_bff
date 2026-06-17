package ru.copperside.payadmin.crossborder.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TransgranEnginePropertiesTest {

    @Test
    void bindsDefaultsAndOverrides() {
        MockEnvironment env = new MockEnvironment();
        env.withProperty("payadmin-bff.transgran-engine.base-url", "http://transgran-engine:8089");
        env.withProperty("payadmin-bff.transgran-engine.internal-admin-api-key", "secret");
        Binder binder = new Binder(ConfigurationPropertySources.get(env));

        TransgranEngineProperties props = binder
                .bind("payadmin-bff.transgran-engine", TransgranEngineProperties.class)
                .get();

        assertThat(props.baseUrl()).isEqualTo("http://transgran-engine:8089");
        assertThat(props.internalAdminHeaderName()).isEqualTo("X-Internal-Admin-Key");
        assertThat(props.internalAdminApiKey()).isEqualTo("secret");
        assertThat(props.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(props.readTimeout()).isEqualTo(Duration.ofSeconds(5));
    }
}
