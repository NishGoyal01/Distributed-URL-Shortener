package com.example.urlshortener.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimiterServiceTest {

    @Test
    void shouldAllowRequestsWithinLimit() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:127.0.0.1")).thenReturn(1L, 2L, 3L);
        when(redisTemplate.expire("rate-limit:127.0.0.1", java.time.Duration.ofSeconds(60))).thenReturn(true);

        RateLimiterService limiter = new RateLimiterService(redisTemplate, 3, 60);

        assertTrue(limiter.allowRequest("127.0.0.1"));
        assertTrue(limiter.allowRequest("127.0.0.1"));
        assertTrue(limiter.allowRequest("127.0.0.1"));
    }

    @Test
    void shouldRejectWhenRateLimitExceeded() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:127.0.0.1")).thenReturn(4L);
        when(redisTemplate.expire("rate-limit:127.0.0.1", java.time.Duration.ofSeconds(60))).thenReturn(true);

        RateLimiterService limiter = new RateLimiterService(redisTemplate, 3, 60);

        assertFalse(limiter.allowRequest("127.0.0.1"));
    }
}
