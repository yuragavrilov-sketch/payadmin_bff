package ru.copperside.payadmin.limitprojection.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payadmin-bff.pay-limit-projection")
public record LimitProjectionProperties(
        @NotBlank @DefaultValue("http://localhost:8088") String baseUrl,
        @NotBlank @DefaultValue("X-Internal-Admin-Key") String internalAdminHeaderName,
        @DefaultValue("") String internalAdminApiKey,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout
) {
}
