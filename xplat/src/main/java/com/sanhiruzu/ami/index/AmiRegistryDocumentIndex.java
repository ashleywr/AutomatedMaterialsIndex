package com.sanhiruzu.ami.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AmiRegistryDocumentIndex {
    public static final AmiRegistryDocumentIndex EMPTY = new AmiRegistryDocumentIndex(List.of());

    private final List<RegistryDocument> documents;

    public AmiRegistryDocumentIndex(List<RegistryDocument> documents) {
        this.documents = List.copyOf(documents);
    }

    public List<RegistryDocument> allDocuments() {
        return documents;
    }

    /**
     * Returns documents matching the query text, filtered to the given kinds.
     * Handles $kind prefix (strips $, matches kind token), # prefix (strips #, matches tag id),
     * ~ prefix (strips ~, matches game rule id), and plain text.
     */
    public List<RegistryDocument> query(String rawQuery, Set<RegistryDocumentKind> enabledKinds) {
        if (rawQuery == null || rawQuery.isBlank() || enabledKinds.isEmpty()) {
            return List.of();
        }

        String query = rawQuery.strip();
        // Strip $ prefix — matches kind token baked into each document
        if (query.startsWith("$")) {
            query = query.substring(1);
        }
        // Strip # prefix — tag id search
        else if (query.startsWith("#")) {
            query = query.substring(1);
        }
        // Strip ~ prefix — game rule search
        else if (query.startsWith("~")) {
            query = query.substring(1);
        }

        if (query.isBlank()) {
            return List.of();
        }

        List<String> terms = tokenize(query);
        List<RegistryDocument> results = new ArrayList<>();
        for (RegistryDocument doc : documents) {
            if (!enabledKinds.contains(doc.kind())) continue;
            if (matches(doc, terms)) {
                results.add(doc);
            }
        }
        return List.copyOf(results);
    }

    private static boolean matches(RegistryDocument doc, List<String> terms) {
        String haystack = normalize(doc.displayName() + " " + doc.description() + " " + doc.id()
                + " " + String.join(" ", doc.searchTokens()));
        for (String term : terms) {
            if (!haystack.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        for (String part : normalize(query).split("\\s+")) {
            if (!part.isBlank()) tokens.add(part);
        }
        return tokens;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replace('/', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
