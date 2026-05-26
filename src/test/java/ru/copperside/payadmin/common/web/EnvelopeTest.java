package ru.copperside.payadmin.common.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvelopeTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-25T20:00:00Z"), ZoneOffset.UTC);

    @Test
    void successEnvelopeContainsDataMetaNullErrorAndTimestamp() {
        ApiMeta meta = new ApiMeta(100, 0, 1, null, null, null, "id", "asc");

        ApiResponse<List<String>> response = ApiResponse.success(List.of("ok"), meta, CLOCK);

        assertThat(response.data()).containsExactly("ok");
        assertThat(response.meta()).isEqualTo(meta);
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isEqualTo(Instant.parse("2026-05-25T20:00:00Z"));
    }

    @Test
    void problemEnvelopeContainsProblemAndTimestamp() {
        ProblemDetail detail = new ProblemDetail(
                "https://contracts.newpay/errors/unauthorized",
                "Unauthorized",
                401,
                "UNAUTHORIZED",
                "Bearer token missing or invalid",
                null,
                "00000000-0000-0000-0000-000000000000"
        );

        ProblemEnvelope envelope = ProblemEnvelope.of(detail, CLOCK);

        assertThat(envelope.error()).isEqualTo(detail);
        assertThat(envelope.timestamp()).isEqualTo(Instant.parse("2026-05-25T20:00:00Z"));
    }
}

