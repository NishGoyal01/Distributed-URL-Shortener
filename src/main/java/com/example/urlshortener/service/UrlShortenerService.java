package com.example.urlshortener.service;

import com.example.urlshortener.cache.UrlCacheEntry;
import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.dto.UrlStatsResponse;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.exception.ExpiredUrlException;
import com.example.urlshortener.exception.NotFoundException;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.UrlValidator;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    private final UrlRepository urlRepository;
    private final RateLimiterService rateLimiterService;
    private final UrlCacheService urlCacheService;
    private final UrlCreationTransaction urlCreationTransaction;
    private final String baseUrl;

    private static final int MAX_CREATE_ATTEMPTS = 5;
    private static final String NORMALIZED_URL_CONSTRAINT = "uq_urls_normalized_original_url";
    private static final String SHORT_CODE_CONSTRAINT = "uq_urls_short_code";

    public UrlShortenerService(
            UrlRepository urlRepository,
            RateLimiterService rateLimiterService,
            UrlCacheService urlCacheService,
            UrlCreationTransaction urlCreationTransaction,
            @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.rateLimiterService = rateLimiterService;
        this.urlCacheService = urlCacheService;
        this.urlCreationTransaction = urlCreationTransaction;
        this.baseUrl = baseUrl;
    }

    public UrlResponse createUrl(CreateUrlRequest request, String clientIp) {
        if (!rateLimiterService.allowRequest(clientIp)) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }

        String originalUrl = request.originalUrl() == null ? null : request.originalUrl().strip();
        UrlValidator.validateOriginalUrl(originalUrl);
        Instant expiresAt = request.expiresAt();
        if (expiresAt != null) {
            UrlValidator.validateExpiration(expiresAt);
        }

        UrlEntity saved = null;
        DataIntegrityViolationException lastConflict = null;
        for (int attempt = 0; attempt < MAX_CREATE_ATTEMPTS; attempt++) {
            try {
                saved = urlCreationTransaction.createOrFind(originalUrl, expiresAt);
                break;
            } catch (DataIntegrityViolationException exception) {
                String constraintName = findConstraintName(exception);
                if (NORMALIZED_URL_CONSTRAINT.equals(constraintName)) {
                    Optional<UrlEntity> existing = urlCreationTransaction.findActive(originalUrl);
                    if (existing.isPresent()) {
                        saved = existing.get();
                        break;
                    }
                    lastConflict = exception;
                } else if (SHORT_CODE_CONSTRAINT.equals(constraintName)) {
                    lastConflict = exception;
                } else {
                    throw exception;
                }
            }
        }

        if (saved == null) {
            throw new IllegalStateException("Unable to allocate a unique short code after " + MAX_CREATE_ATTEMPTS + " attempts", lastConflict);
        }

        String resolvedBaseUrl = baseUrl == null || baseUrl.isBlank()
            ? ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            : baseUrl;
        resolvedBaseUrl = resolvedBaseUrl.replaceAll("/+$", "");

        return new UrlResponse(saved.getShortCode(), resolvedBaseUrl + "/" + saved.getShortCode(), saved.getOriginalUrl(), saved.getCreatedAt(), saved.getExpiresAt());
    }

    public String redirect(String shortCode) {
        UrlValidator.validateShortCode(shortCode);

        Optional<UrlCacheEntry> cached = urlCacheService.get(shortCode);
        if (cached.isPresent()) {
            UrlCacheEntry entry = cached.get();
            log.info("Redis cache hit for shortCode={} at {}", shortCode, Instant.now());
            if (entry.expiresAt() != null && entry.expiresAt().isBefore(Instant.now())) {
                log.warn("Redis cache entry expired for shortCode={}", shortCode);
                throw new ExpiredUrlException("URL expired");
            }
            urlRepository.incrementClickStats(shortCode, Instant.now());
            log.info("Incremented PostgreSQL analytics for shortCode={} after cache hit", shortCode);
            return entry.originalUrl();
        }

        log.info("Redis cache miss for shortCode={}, loading from PostgreSQL", shortCode);
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new NotFoundException("Short URL not found"));

        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
            log.warn("PostgreSQL record expired for shortCode={}", shortCode);
            throw new ExpiredUrlException("URL expired");
        }

        urlCacheService.put(shortCode, entity, entity.getExpiresAt());
        urlRepository.incrementClickStats(shortCode, Instant.now());
        log.info("Stored shortCode={} in Redis cache and updated PostgreSQL analytics", shortCode);
        return entity.getOriginalUrl();
    }

    public UrlStatsResponse getStats(String shortCode) {
        UrlValidator.validateShortCode(shortCode);
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new NotFoundException("Short URL not found"));

        boolean active = entity.getExpiresAt() == null || !entity.getExpiresAt().isBefore(Instant.now());

        return new UrlStatsResponse(
            entity.getShortCode(),
            entity.getOriginalUrl(),
            entity.getCreatedAt(),
            entity.getExpiresAt(),
            entity.getClickCount(),
            entity.getLastAccessedAt(),
            active
        );
    }

    public void deleteUrl(String shortCode) {
        UrlValidator.validateShortCode(shortCode);
        urlCreationTransaction.delete(shortCode);
    }

    private String findConstraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            current = current.getCause();
        }
        return null;
    }
}
