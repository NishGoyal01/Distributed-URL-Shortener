package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank(message = "originalUrl is required")
        String originalUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant expiresAt
) {
    public CreateUrlRequest {
        originalUrl = originalUrl == null ? null : originalUrl.strip();
    }
}
