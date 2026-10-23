package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Base62Test {

    @Test
    void shouldEncodePositiveValuesDeterministically() {
        Base62 base62 = new Base62();

        assertEquals("0", base62.encode(0L));
        assertEquals("1", base62.encode(1L));
        assertEquals("A", base62.encode(10L));
        assertEquals("Z", base62.encode(35L));
        assertEquals("a", base62.encode(36L));
        assertEquals("10", base62.encode(62L));
        assertEquals("8M0kX", base62.encode(123456789L));
    }

    @Test
    void shouldRejectNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new Base62().encode(-1L));
    }
}
