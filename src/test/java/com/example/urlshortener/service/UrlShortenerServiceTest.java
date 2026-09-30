package com.example.urlshortener.service;

import com.example.urlshortener.dto.CreateUrlRequest;
import com.example.urlshortener.entity.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.service.UrlCreationTransaction;
import com.example.urlshortener.util.Base62;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UrlShortenerServiceTest {

    private UrlRepository urlRepository;
    private UrlCreationTransaction urlCreationTransaction;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        urlRepository = mock(UrlRepository.class);
        RateLimiterService rateLimiterService = mock(RateLimiterService.class);
        urlCreationTransaction = mock(UrlCreationTransaction.class);
        when(rateLimiterService.allowRequest("127.0.0.1")).thenReturn(true);
        service = new UrlShortenerService(urlRepository, rateLimiterService, mock(UrlCacheService.class), urlCreationTransaction, "https://links.example.com///");
    }

    @Test
    void shouldCreateUrlWithGeneratedShortCode() {
        UrlEntity entity = new UrlEntity();
        entity.setId(123L);
        entity.setShortCode("a7K2mQ");
        entity.setOriginalUrl("https://example.com/long/path");
        entity.setNormalizedOriginalUrl("https://example.com/long/path");
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(Instant.parse("2026-12-31T23:59:59Z"));

        when(urlCreationTransaction.createOrFind("https://example.com/long/path", entity.getExpiresAt())).thenReturn(entity);

        var response = service.createUrl(new CreateUrlRequest("  https://example.com/long/path  ", entity.getExpiresAt()), "127.0.0.1");

        assertNotNull(response);
        assertEquals("https://links.example.com/a7K2mQ", response.shortUrl());
        assertEquals("https://example.com/long/path", response.originalUrl());
        verify(urlCreationTransaction).createOrFind("https://example.com/long/path", entity.getExpiresAt());
    }

    @Test
    void shouldRejectPastExpiration() {
        var request = new CreateUrlRequest("https://example.com/long/path", Instant.now().minusSeconds(60));

        var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.createUrl(request, "127.0.0.1"));
        assertEquals("expiresAt must be in the future", ex.getMessage());
        verify(urlCreationTransaction, never()).createOrFind(any(), any());
    }
}
