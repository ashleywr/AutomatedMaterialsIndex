package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight lexical index for client-visible advancement documents.
 */
public final class AmiAdvancementSearchIndex {
    private final List<AmiAdvancementDocument> documents = new ArrayList<>();
    private final Map<AmiAdvancementDocument, String> searchableText = new LinkedHashMap<>();

    public AmiAdvancementSearchIndex(Collection<AmiAdvancementDocument> documents) {
        if (documents == null) {
            return;
        }
        for (AmiAdvancementDocument document : documents) {
            if (document == null) {
                continue;
            }
            this.documents.add(document);
            searchableText.put(document, buildSearchableText(document));
        }
        this.documents.sort(Comparator.comparing(document -> document.id().toString()));
    }

    public List<AmiAdvancementDocument> allDocuments() {
        return List.copyOf(documents);
    }

    public List<AmiAdvancementDocument> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<ScoredAdvancement> scored = new ArrayList<>();
        for (AmiAdvancementDocument document : documents) {
            String haystack = searchableText.getOrDefault(document, "");
            boolean matched = true;
            int score = 0;
            for (String token : tokens) {
                if (!haystack.contains(token)) {
                    matched = false;
                    break;
                }
                score += scoreToken(document, token);
            }
            if (matched) {
                scored.add(new ScoredAdvancement(document, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredAdvancement::score).reversed()
                .thenComparingInt(scoredAdvancement -> progressSort(scoredAdvancement.document()))
                .thenComparing(scoredAdvancement -> scoredAdvancement.document().id().toString()));
        List<AmiAdvancementDocument> results = new ArrayList<>();
        for (ScoredAdvancement scoredAdvancement : scored) {
            results.add(scoredAdvancement.document());
        }
        return results;
    }

    private static String buildSearchableText(AmiAdvancementDocument document) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.add(document.id().toString());
        fields.add(document.id().getNamespace());
        fields.add(document.id().getPath());
        fields.add(document.sourceId());
        fields.add(document.tabTitle());
        fields.add(document.title());
        fields.add(document.description());
        fields.add(document.type());
        fields.add(document.progressStatus().label());
        if (document.iconItemId() != null) {
            fields.add(document.iconItemId().toString());
            fields.add(document.iconItemId().getNamespace());
            fields.add(document.iconItemId().getPath());
        }
        return normalize(String.join(" ", fields));
    }

    private static int scoreToken(AmiAdvancementDocument document, String token) {
        int score = 0;
        if (contains(document.title(), token)) score += 50;
        if (contains(document.tabTitle(), token)) score += 35;
        if (contains(document.sourceId(), token)) score += 25;
        if (contains(document.id().toString(), token) || contains(document.id().getPath(), token)) score += 25;
        if (contains(document.type(), token)) score += 15;
        if (document.iconItemId() != null
                && (contains(document.iconItemId().toString(), token) || contains(document.iconItemId().getPath(), token))) {
            score += 15;
        }
        if (contains(document.description(), token)) score += 5;
        return score;
    }

    private static int progressSort(AmiAdvancementDocument document) {
        return switch (document.progressStatus()) {
            case IN_PROGRESS -> 0;
            case NOT_STARTED -> 1;
            case UNKNOWN -> 2;
            case COMPLETED -> 3;
        };
    }

    private static boolean contains(String value, String token) {
        return normalize(value).contains(token);
    }

    private static List<String> tokens(String query) {
        List<String> tokens = new ArrayList<>();
        for (String token : normalize(query).split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(':', ' ')
                .replace('/', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record ScoredAdvancement(AmiAdvancementDocument document, int score) {
    }
}
