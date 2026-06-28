package com.miniopgg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riot")
public record RiotApiProperties(
        String apiKey,
        String regionalRoute,
        String platformRoute,
        long cacheTtlMinutes
) {
}
