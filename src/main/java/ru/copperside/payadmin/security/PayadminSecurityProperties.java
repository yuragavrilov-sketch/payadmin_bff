package ru.copperside.payadmin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payadmin-bff.security")
public record PayadminSecurityProperties(
        @DefaultValue("") String requiredAuthority
) {
    public boolean hasRequiredAuthority() {
        return requiredAuthority != null && !requiredAuthority.isBlank();
    }
}

