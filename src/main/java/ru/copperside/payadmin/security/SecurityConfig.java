package ru.copperside.payadmin.security;

import jakarta.servlet.http.HttpServletResponse;
import ru.copperside.payadmin.common.web.ProblemDetail;
import ru.copperside.payadmin.common.web.ProblemEnvelope;
import ru.copperside.payadmin.common.web.RequestIdFilter;
import ru.copperside.payadmin.security.PayadminSecurityProperties;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    private final PayadminSecurityProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SecurityConfig(PayadminSecurityProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
                    if (properties.hasRequiredAuthority()) {
                        auth.requestMatchers("/api/**").hasAuthority(properties.requiredAuthority());
                    } else {
                        auth.requestMatchers("/api/**").authenticated();
                    }
                    auth.anyRequest().permitAll();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> writeProblem(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHORIZED",
                                "unauthorized",
                                "Unauthorized",
                                "Bearer token missing or invalid"
                        ))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeProblem(
                                response,
                                HttpStatus.FORBIDDEN,
                                "FORBIDDEN",
                                "forbidden",
                                "Forbidden",
                                "Required authority is missing"
                        ))
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    private void writeProblem(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String typeSuffix,
            String title,
            String message
    ) throws IOException {
        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + typeSuffix,
                title,
                status.value(),
                code,
                message,
                null,
                traceId()
        );

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ProblemEnvelope.of(detail, clock));
    }

    private String traceId() {
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}

