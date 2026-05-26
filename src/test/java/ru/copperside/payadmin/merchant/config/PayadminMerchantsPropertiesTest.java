package ru.copperside.payadmin.merchant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PayadminMerchantsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsDefaultUnknownMcc() {
        contextRunner.run(context -> {
            PayadminMerchantsProperties props = context.getBean(PayadminMerchantsProperties.class);

            assertThat(props.unknownMcc()).isEqualTo("0000");
        });
    }

    @Test
    void bindsConfiguredUnknownMcc() {
        contextRunner
                .withPropertyValues("payadmin-bff.merchants.unknown-mcc=9999")
                .run(context -> {
                    PayadminMerchantsProperties props = context.getBean(PayadminMerchantsProperties.class);

                    assertThat(props.unknownMcc()).isEqualTo("9999");
                });
    }

    @Test
    void failsValidationForInvalidUnknownMcc() {
        contextRunner
                .withPropertyValues("payadmin-bff.merchants.unknown-mcc=bad")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(BindValidationException.class)
                        .hasStackTraceContaining("unknownMcc"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PayadminMerchantsProperties.class)
    static class PropertiesConfiguration {
    }
}

