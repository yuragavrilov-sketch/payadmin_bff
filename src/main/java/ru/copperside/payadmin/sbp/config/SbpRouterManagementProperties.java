package ru.copperside.payadmin.sbp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payadmin-bff.sbp-router-management")
public record SbpRouterManagementProperties(
        @NotBlank @DefaultValue("http://localhost:8087") String baseUrl,
        @NotBlank @DefaultValue("X-Internal-Admin-Key") String internalAdminHeaderName,
        @DefaultValue("") String internalAdminApiKey,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout
) {
}
