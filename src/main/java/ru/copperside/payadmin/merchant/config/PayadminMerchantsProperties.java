package ru.copperside.payadmin.merchant.config;

import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "payadmin-bff.merchants")
public record PayadminMerchantsProperties(
        @Pattern(regexp = "\\d{4}") @DefaultValue("0000") String unknownMcc
) {
}

