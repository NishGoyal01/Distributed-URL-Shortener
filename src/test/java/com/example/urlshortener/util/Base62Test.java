package com.example.urlshortener.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Base62Test {

    @Test
    void shouldEncodePositiveValuesDeterministically() {
        Base62 base62 = new Base62();

        assertEquals("0", base62.encode(0L));
        assertEquals("1", base62.encode(1L));
        assertEquals("A", base62.encode(10L));
        assertEquals("Z", base62.encode(35L));
        assertEquals("a", base62.encode(36L));
        assertEquals("8M0kX", base62.encode(123456789L));
    }

    @Test
    void shouldGenerateSixCharacterMixedBase62Codes() {
        Base62 base62 = new Base62();

        for (int iteration = 0; iteration < 100; iteration++) {
            String code = base62.generateCode();
            assertTrue(code.length() >= 6);
            assertTrue(code.matches("[0-9A-Za-z]+"));
            assertTrue(code.matches(".*[A-Za-z].*"));
            assertTrue(code.matches(".*[0-9].*"));
        }
    }
}
