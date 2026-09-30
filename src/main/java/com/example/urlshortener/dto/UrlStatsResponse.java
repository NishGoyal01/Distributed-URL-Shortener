package com.example.urlshortener.dto;

import java.time.Instant;

public record UrlStatsResponse(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount,
        Instant lastAccessedAt,
        boolean active
) {
}
