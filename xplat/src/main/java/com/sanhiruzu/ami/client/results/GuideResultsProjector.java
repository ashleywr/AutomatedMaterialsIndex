package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts guide index hits into row data that the results UI can render
 * without knowing the source guidebook implementation.
 */
public final class GuideResultsProjector {
    private GuideResultsProjector() {
    }

    public static List<GuideResultRow> project(String query, AmiGuideSearchIndex guideIndex) {
        if (guideIndex == null || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<GuideResultRow> rows = new ArrayList<>();
        for (AmiGuideDocument document : guideIndex.search(query)) {
            List<MatchEvidence> evidence = evidence(document, tokens);
            rows.add(new GuideResultRow(
                    document,
                    document.title(),
                    sourceLine(document),
                    provenanceLine(document),
                    document.referencedItems().size(),
                    evidence
            ));
        }
        return rows;
    }

    private static String sourceLine(AmiGuideDocument document) {
        List<String> parts = new ArrayList<>();
        if (!document.modId().isBlank()) {
            parts.add(ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(document.modId(), true)));
        }
        if (document.bookId() != null) {
            parts.add(formatId(document.bookId()));
        }
        if (!document.chapter().isBlank()) {
            parts.add(document.chapter());
        }
        parts.add("Guide Page");
        return String.join(" > ", parts);
    }

    private static String provenanceLine(AmiGuideDocument document) {
        if (document.referencedItems().isEmpty()) {
            return "Guide match";
        }
        if (document.referencedItems().size() == 1) {
            return "Guide Page - mentions " + formatId(document.referencedItems().get(0));
        }
        return "Guide Page - mentions " + document.referencedItems().size() + " items";
    }

    private static List<MatchEvidence> evidence(AmiGuideDocument document, List<String> tokens) {
        List<MatchEvidence> evidence = new ArrayList<>();
        addEvidence(evidence, tokens, document.title(), MatchEvidence.SourceType.GUIDE_TITLE, document.id(), document.title(), "");
        addEvidence(evidence, tokens, document.chapter(), MatchEvidence.SourceType.GUIDE_CHAPTER, document.id(), document.chapter(), "");
        for (String tag : document.tags()) {
            addEvidence(evidence, tokens, tag, MatchEvidence.SourceType.GUIDE_TAG, document.id(), tag, "");
        }
        for (ResourceLocation itemId : document.referencedItems()) {
            String itemLabel = formatId(itemId);
            addEvidence(evidence, tokens, itemId.toString() + " " + itemId.getPath(), MatchEvidence.SourceType.GUIDE_REFERENCE,
                    document.id(), itemLabel, "");
        }
        String snippet = matchedSnippet(document.summaryText(), tokens);
        if (!snippet.isBlank()) {
            evidence.add(new MatchEvidence(MatchEvidence.SourceType.GUIDE_SUMMARY, document.id(), document.title(), snippet));
        }
        return List.copyOf(evidence);
    }

    private static void addEvidence(List<MatchEvidence> evidence, List<String> tokens, String haystack,
                                    MatchEvidence.SourceType sourceType, ResourceLocation sourceId,
                                    String label, String snippet) {
        String normalized = normalize(haystack);
        if (normalized.isBlank()) {
            return;
        }
        for (String token : tokens) {
            if (normalized.contains(token)) {
                evidence.add(new MatchEvidence(sourceType, sourceId, label, snippet));
                return;
            }
        }
    }

    private static String matchedSnippet(String text, List<String> tokens) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = normalize(text);
        for (String token : tokens) {
            int index = normalized.indexOf(token);
            if (index >= 0) {
                int start = Math.max(0, index - 40);
                int end = Math.min(text.length(), index + token.length() + 60);
                return text.substring(start, end).trim();
            }
        }
        return "";
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

    private static String formatId(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        return ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(id.toString(), true));
    }
}
