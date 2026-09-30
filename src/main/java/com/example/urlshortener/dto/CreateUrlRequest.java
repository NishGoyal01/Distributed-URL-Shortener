package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank(message = "originalUrl is required")
        @Pattern(regexp = "https?:\\/\\/.*", message = "originalUrl must be a valid absolute http or https URL")
        String originalUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant expiresAt
) {
        public CreateUrlRequest {
                originalUrl = originalUrl == null ? null : originalUrl.strip();
        }
}
