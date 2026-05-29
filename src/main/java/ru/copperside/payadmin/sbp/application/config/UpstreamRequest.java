package ru.copperside.payadmin.sbp.application.config;

public record UpstreamRequest(
        String name, String url, Integer timeoutMs, Integer retryMaxAttempts, Integer retryBackoffMs) {
}
