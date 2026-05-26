package ru.copperside.payadmin.merchant.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "payadmin-bff.merchants-core")
public record MerchantsCoreProperties(
        @NotBlank @DefaultValue("http://localhost:8082") String baseUrl,
        @NotBlank @DefaultValue("X-Internal-Admin-Key") String internalAdminHeaderName,
        @DefaultValue("") String internalAdminApiKey,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("5s") Duration readTimeout,
        @Min(1) @Max(500) @DefaultValue("500") int pageSize,
        @Min(1) @Max(1000) @DefaultValue("20") int maxPages
) {
}

