package com.example.urlshortener.service;

import com.example.urlshortener.cache.UrlCacheEntry;
import com.example.urlshortener.entity.UrlEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final long cacheTtlSeconds;

    public UrlCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${app.cache.ttl-seconds}") long cacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Optional<UrlCacheEntry> get(String shortCode) {
        String value = redisTemplate.opsForValue().get(key(shortCode));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            UrlCacheEntry entry = objectMapper.readValue(value, UrlCacheEntry.class);
            return Optional.of(entry);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void put(String shortCode, UrlEntity entity, Instant expiresAt) {
        Instant now = Instant.now();
        Instant effectiveExpiry = expiresAt != null ? expiresAt : now.plusSeconds(cacheTtlSeconds);
        Duration defaultTtl = Duration.ofSeconds(cacheTtlSeconds);
        Duration remaining = expiresAt != null ? Duration.between(now, expiresAt) : defaultTtl;
        Duration ttl = remaining.compareTo(defaultTtl) < 0 ? remaining : defaultTtl;
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(1);
        }
        UrlCacheEntry entry = new UrlCacheEntry(shortCode, entity.getOriginalUrl(), effectiveExpiry);
        try {
            redisTemplate.opsForValue().set(key(shortCode), objectMapper.writeValueAsString(entry), ttl);
            log.info("URL cached in Redis for shortCode={} ttlSeconds={} expiresAt={}", shortCode, ttl.getSeconds(), effectiveExpiry);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize URL cache entry", e);
        }
    }

    public void delete(String shortCode) {
        redisTemplate.delete(key(shortCode));
    }

    private String key(String shortCode) {
        return "url:" + shortCode;
    }
}
