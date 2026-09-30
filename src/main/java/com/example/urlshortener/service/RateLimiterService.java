package com.example.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final long maxRequests;
    private final long windowSeconds;

    public RateLimiterService(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.max-requests}") long maxRequests,
            @Value("${app.rate-limit.window-seconds}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public boolean allowRequest(String clientIp) {
        String key = "rate-limit:" + clientIp;
        Long current = redisTemplate.opsForValue().increment(key);
        if (current == null || current <= 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        return current <= maxRequests;
    }
}
