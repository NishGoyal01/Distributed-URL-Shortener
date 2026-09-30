package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidatorTest {

    @Test
    void shouldAcceptValidHttpAndHttpsUrls() {
        assertDoesNotThrow(() -> UrlValidator.validateOriginalUrl("https://example.com/path?q=1"));
        assertDoesNotThrow(() -> UrlValidator.validateOriginalUrl("http://localhost:8080/test"));
    }

    @Test
    void shouldRejectMalformedUrls() {
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validateOriginalUrl("not-a-url"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validateOriginalUrl("ftp://example.com"));
    }

    @Test
    void shouldValidateShortCodes() {
        assertDoesNotThrow(() -> UrlValidator.validateShortCode("aB92xK"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validateShortCode("bad code"));
        assertThrows(IllegalArgumentException.class, () -> UrlValidator.validateShortCode(""));
    }
}
