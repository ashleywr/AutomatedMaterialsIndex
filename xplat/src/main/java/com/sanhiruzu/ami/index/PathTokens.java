package com.sanhiruzu.ami.index;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Exact token/phrase matching for registry paths, tag ids, and normalized metadata.
 * Use this for fallback lexical evidence so "gear" does not match "gearbox"
 * unless the rule explicitly asks for both.
 */
public final class PathTokens {
    private static final ConcurrentMap<String, PathTokens> CACHE = new ConcurrentHashMap<>();
    private final String raw;
    private final List<String> parts;
    private final Set<String> partSet;

    private PathTokens(String value) {
        this.raw = normalize(value);
        this.parts = split(this.raw);
        this.partSet = new LinkedHashSet<>(this.parts);
    }

    private PathTokens(String normalized, boolean alreadyNormalized) {
        this.raw = alreadyNormalized ? normalized : normalize(normalized);
        this.parts = split(this.raw);
        this.partSet = new LinkedHashSet<>(this.parts);
    }

    public static PathTokens of(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return new PathTokens("");
        }
        return CACHE.computeIfAbsent(normalized, key -> new PathTokens(key, true));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static List<String> split(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : normalized.split(" ")) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    public boolean contains(String tokenOrPhrase) {
        List<String> expected = split(normalize(tokenOrPhrase));
        if (expected.isEmpty()) {
            return false;
        }
        if (expected.size() == 1) {
            return partSet.contains(expected.get(0));
        }
        return containsPhrase(expected);
    }

    public boolean containsAny(String... values) {
        for (String value : values) {
            if (contains(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAny(Collection<String> values) {
        for (String value : values) {
            if (contains(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(String... values) {
        for (String value : values) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(String tokenOrPhrase) {
        List<String> expected = split(normalize(tokenOrPhrase));
        if (expected.isEmpty() || expected.size() > parts.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!parts.get(i).equals(expected.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(String tokenOrPhrase) {
        List<String> expected = split(normalize(tokenOrPhrase));
        if (expected.isEmpty() || expected.size() > parts.size()) {
            return false;
        }
        int offset = parts.size() - expected.size();
        for (int i = 0; i < expected.size(); i++) {
            if (!parts.get(offset + i).equals(expected.get(i))) {
                return false;
            }
        }
        return true;
    }

    public String raw() {
        return raw;
    }

    private boolean containsPhrase(List<String> expected) {
        if (expected.size() > parts.size()) {
            return false;
        }
        for (int start = 0; start <= parts.size() - expected.size(); start++) {
            boolean matched = true;
            for (int i = 0; i < expected.size(); i++) {
                if (!parts.get(start + i).equals(expected.get(i))) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }
}
