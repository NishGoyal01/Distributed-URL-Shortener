package com.example.urlshortener.cache;

import java.time.Instant;

public record UrlCacheEntry(String shortCode, String originalUrl, Instant expiresAt) {
}
