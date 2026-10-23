package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

@Component
public class Base62 {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative");
        }
        if (value == 0) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        long current = value;
        while (current > 0) {
            long remainder = current % 62L;
            builder.append(ALPHABET.charAt((int) remainder));
            current = current / 62L;
        }
        return builder.reverse().toString();
    }
}
