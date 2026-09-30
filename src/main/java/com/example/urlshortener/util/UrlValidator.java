package com.example.urlshortener.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

public final class UrlValidator {

    private UrlValidator() {
    }

    public static void validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl is required");
        }

        URI uri;
        try {
            uri = new URI(originalUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("originalUrl must be a valid absolute URL");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || host == null || host.isBlank()) {
            throw new IllegalArgumentException("originalUrl must be a valid absolute http or https URL");
        }
    }

    public static void validateExpiration(Instant expiresAt) {
        if (expiresAt == null) {
            return;
        }
        if (expiresAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
    }

    public static void validateShortCode(String shortCode) {
        if (shortCode == null || shortCode.isBlank()) {
            throw new IllegalArgumentException("shortCode is required");
        }
        if (!shortCode.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("shortCode format is invalid");
        }
    }
}
