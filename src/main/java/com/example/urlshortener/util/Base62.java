package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base62 {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        String candidate;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int index = 0; index < CODE_LENGTH; index++) {
                builder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
            }
            candidate = builder.toString();
        } while (!candidate.matches("(?=.*[A-Za-z])(?=.*[0-9])[0-9A-Za-z]{6}"));

        return candidate;
    }

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
