package ru.copperside.payadmin.crossborder.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payadmin-bff.transgran-engine")
public record TransgranEngineProperties(
        @NotBlank @DefaultValue("http://localhost:8089") String baseUrl,
        @NotBlank @DefaultValue("X-Internal-Admin-Key") String internalAdminHeaderName,
        @DefaultValue("") String internalAdminApiKey,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout
) {
}
