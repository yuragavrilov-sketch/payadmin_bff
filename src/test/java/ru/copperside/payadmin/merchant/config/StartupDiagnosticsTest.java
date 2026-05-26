package ru.copperside.payadmin.merchant.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void diagnosticsDoNotLogRawInternalAdminKey(CapturedOutput output) {
        MerchantsCoreProperties coreProperties = new MerchantsCoreProperties(
                "http://localhost:8082",
                "X-Internal-Admin-Key",
                "super-secret-key",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                500,
                20
        );
        StartupDiagnostics diagnostics = new StartupDiagnostics(
                new MockEnvironment().withProperty("spring.profiles.active", "local"),
                coreProperties,
                new PayadminMerchantsProperties("0000")
        );

        diagnostics.run(new DefaultApplicationArguments());

        assertThat(output).contains("internalAdminKeyConfigured=true");
        assertThat(output).doesNotContain("super-secret-key");
    }
}
