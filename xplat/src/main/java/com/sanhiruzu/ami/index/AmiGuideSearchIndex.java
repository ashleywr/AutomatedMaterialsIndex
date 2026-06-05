package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight lexical index for guide documents.
 * <p>
 * This intentionally stays separate from {@link SearchIndex}; guide text can be
 * larger and lower-priority than normal item metadata.
 */
public final class AmiGuideSearchIndex {
    public static final String GUIDEBOOKS_FILTER_TOKEN = "guidebooks";
    public static final int DEFAULT_SUMMARY_TEXT_CAP = 4096;

    private final GuideIndexingMode mode;
    private final int summaryTextCap;
    private final List<AmiGuideDocument> documents = new ArrayList<>();
    private final Map<AmiGuideDocument, String> searchableText = new LinkedHashMap<>();

    public AmiGuideSearchIndex(Collection<AmiGuideDocument> documents, GuideIndexingMode mode) {
        this(documents, mode, DEFAULT_SUMMARY_TEXT_CAP);
    }

    public AmiGuideSearchIndex(Collection<AmiGuideDocument> documents, GuideIndexingMode mode, int summaryTextCap) {
        this.mode = mode == null ? GuideIndexingMode.TITLES : mode;
        this.summaryTextCap = Math.max(0, summaryTextCap);
        if (this.mode == GuideIndexingMode.OFF || documents == null) {
            return;
        }
        for (AmiGuideDocument document : documents) {
            if (document == null) {
                continue;
            }
            this.documents.add(document);
            searchableText.put(document, buildSearchableText(document));
        }
        this.documents.sort(Comparator.comparing(document -> document.id().toString()));
    }

    public static AmiGuideSearchIndex fromConfig(Collection<AmiGuideDocument> documents) {
        GuideIndexingMode mode = switch (AmiConfig.guideIndexingMode) {
            case OFF -> GuideIndexingMode.OFF;
            case TITLES -> GuideIndexingMode.TITLES;
            case SUMMARY -> GuideIndexingMode.SUMMARY;
        };
        return new AmiGuideSearchIndex(documents, mode, AmiConfig.guideSummaryTextCap);
    }

    public List<AmiGuideDocument> search(String query) {
        if (mode == GuideIndexingMode.OFF || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        if (tokens.stream().anyMatch(token -> GUIDEBOOKS_FILTER_TOKEN.equals(token))) {
            return List.copyOf(documents);
        }

        List<ScoredDocument> scored = new ArrayList<>();
        for (AmiGuideDocument document : documents) {
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
                scored.add(new ScoredDocument(document, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredDocument::score).reversed()
                .thenComparing(scoredDocument -> scoredDocument.document().id().toString()));
        List<AmiGuideDocument> results = new ArrayList<>();
        for (ScoredDocument scoredDocument : scored) {
            results.add(scoredDocument.document());
        }
        return results;
    }

    public List<AmiGuideDocument> allDocuments() {
        return List.copyOf(documents);
    }

    private String buildSearchableText(AmiGuideDocument document) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.add(document.id().toString());
        fields.add(document.sourceType());
        fields.add(document.modId());
        if (document.bookId() != null) {
            fields.add(document.bookId().toString());
            fields.add(document.bookId().getNamespace());
            fields.add(document.bookId().getPath());
        }
        fields.add(document.pageId());
        fields.add(document.title());
        fields.add(document.chapter());
        for (ResourceLocation itemId : document.referencedItems()) {
            fields.add(itemId.toString());
            fields.add(itemId.getNamespace());
            fields.add(itemId.getPath());
        }
        fields.addAll(document.tags());
        if (mode == GuideIndexingMode.SUMMARY) {
            fields.add(cappedSummary(document.summaryText()));
        }
        return normalize(String.join(" ", fields));
    }

    private String cappedSummary(String summaryText) {
        if (summaryText == null || summaryText.isBlank() || summaryTextCap == 0) {
            return "";
        }
        return summaryText.length() <= summaryTextCap ? summaryText : summaryText.substring(0, summaryTextCap);
    }

    private int scoreToken(AmiGuideDocument document, String token) {
        int score = 0;
        if (contains(document.title(), token)) score += 50;
        if (contains(document.chapter(), token)) score += 30;
        if (contains(document.modId(), token)) score += 20;
        if (document.bookId() != null && contains(document.bookId().toString(), token)) score += 20;
        for (ResourceLocation itemId : document.referencedItems()) {
            if (contains(itemId.toString(), token) || contains(itemId.getPath(), token)) {
                score += 25;
                break;
            }
        }
        for (String tag : document.tags()) {
            if (contains(tag, token)) {
                score += 20;
                break;
            }
        }
        if (mode == GuideIndexingMode.SUMMARY && contains(cappedSummary(document.summaryText()), token)) {
            score += 5;
        }
        return score;
    }

    private static boolean contains(String value, String token) {
        return normalize(value).contains(token);
    }

    private static List<String> tokens(String query) {
        List<String> tokens = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return tokens;
        }
        for (String rawToken : query.split("\\s+")) {
            String stripped = stripQuerySyntax(rawToken);
            for (String token : normalize(stripped).split("\\s+")) {
                if (!token.isBlank()) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    private static String stripQuerySyntax(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        String token = rawToken.trim();
        while (token.startsWith("-")) {
            token = token.substring(1);
        }
        while (!token.isEmpty() && "?~$#@&!><=".indexOf(token.charAt(0)) >= 0) {
            token = token.substring(1);
        }
        return token;
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

    public enum GuideIndexingMode {
        OFF,
        TITLES,
        SUMMARY
    }

    private record ScoredDocument(AmiGuideDocument document, int score) {
    }
}
