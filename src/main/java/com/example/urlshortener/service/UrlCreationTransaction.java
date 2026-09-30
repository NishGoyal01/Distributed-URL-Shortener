package com.example.urlshortener.service;

import com.example.urlshortener.entity.ReleasedShortCodeEntity;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.exception.NotFoundException;
import com.example.urlshortener.repository.ReleasedShortCodeRepository;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.Base62;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UrlCreationTransaction {

    private final UrlRepository urlRepository;
    private final ReleasedShortCodeRepository releasedShortCodeRepository;
    private final UrlCacheService urlCacheService;
    private final Base62 base62;
    private final EntityManager entityManager;

    public UrlCreationTransaction(
            UrlRepository urlRepository,
            ReleasedShortCodeRepository releasedShortCodeRepository,
            UrlCacheService urlCacheService,
            Base62 base62,
            EntityManager entityManager) {
        this.urlRepository = urlRepository;
        this.releasedShortCodeRepository = releasedShortCodeRepository;
        this.urlCacheService = urlCacheService;
        this.base62 = base62;
        this.entityManager = entityManager;
    }

    @Transactional
    public UrlEntity createOrFind(String normalizedUrl, Instant expiresAt) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:normalizedUrl, 0))")
            .setParameter("normalizedUrl", normalizedUrl)
            .getSingleResult();

        Optional<UrlEntity> existing = urlRepository.findByNormalizedOriginalUrlForUpdate(normalizedUrl);
        if (existing.isPresent()) {
            UrlEntity current = existing.get();
            if (current.getExpiresAt() == null || current.getExpiresAt().isAfter(Instant.now())) {
                return current;
            }
            removeAndRelease(current);
        }

        UrlEntity entity = new UrlEntity();
        entity.setOriginalUrl(normalizedUrl);
        entity.setNormalizedOriginalUrl(normalizedUrl);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(expiresAt);
        entity.setShortCode(claimReleasedCode().orElseGet(base62::generateCode));

        return urlRepository.saveAndFlush(entity);
    }

    @Transactional(readOnly = true)
    public Optional<UrlEntity> findActive(String normalizedUrl) {
        return urlRepository.findByNormalizedOriginalUrl(normalizedUrl)
            .filter(entity -> entity.getExpiresAt() == null || entity.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void delete(String shortCode) {
        UrlEntity entity = urlRepository.findByShortCodeForUpdate(shortCode)
            .orElseThrow(() -> new NotFoundException("Short URL not found"));
        removeAndRelease(entity);
    }

    private Optional<String> claimReleasedCode() {
        Optional<ReleasedShortCodeEntity> released = releasedShortCodeRepository.claimNextForUpdate();
        if (released.isEmpty()) {
            return Optional.empty();
        }

        String shortCode = released.get().getShortCode();
        releasedShortCodeRepository.delete(released.get());
        releasedShortCodeRepository.flush();
        return Optional.of(shortCode);
    }

    private void removeAndRelease(UrlEntity entity) {
        urlRepository.delete(entity);
        urlRepository.flush();

        ReleasedShortCodeEntity released = new ReleasedShortCodeEntity();
        released.setShortCode(entity.getShortCode());
        released.setReleasedAt(Instant.now());
        releasedShortCodeRepository.saveAndFlush(released);
        urlCacheService.delete(entity.getShortCode());
    }
}