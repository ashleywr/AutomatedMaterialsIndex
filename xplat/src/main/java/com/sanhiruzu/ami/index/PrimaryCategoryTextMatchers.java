package com.sanhiruzu.ami.index;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PrimaryCategoryTextMatchers {
    private PrimaryCategoryTextMatchers() {
    }

    static boolean hasMetadataToken(Map<String, String> attributes, String key, String expected) {
        return hasCsvToken(attributes.getOrDefault(key, ""), expected);
    }

    static boolean hasCsvToken(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        for (String token : encoded.split(",")) {
            if (expected.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    static boolean containsPathToken(String path, Set<String> expectedTokens) {
        return PathTokens.of(path).containsAny(expectedTokens);
    }

    static boolean endsWithPathToken(String path, String token) {
        return PathTokens.of(path).endsWith(token);
    }

    static boolean containsAnyIgnoreCase(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
