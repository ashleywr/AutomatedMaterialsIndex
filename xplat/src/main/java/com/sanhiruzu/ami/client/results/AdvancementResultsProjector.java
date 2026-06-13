package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.index.AmiAdvancementSearchIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts advancement index hits into row data that the results UI can render.
 */
public final class AdvancementResultsProjector {
    private AdvancementResultsProjector() {
    }

    public static List<AdvancementResultRow> project(String query, AmiAdvancementSearchIndex advancementIndex) {
        if (advancementIndex == null || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<AdvancementResultRow> rows = new ArrayList<>();
        for (AmiAdvancementDocument document : advancementIndex.search(query)) {
            rows.add(new AdvancementResultRow(
                    document,
                    document.title(),
                    sourceLine(document),
                    provenanceLine(document),
                    evidence(document, tokens)
            ));
        }
        return rows;
    }

    private static String sourceLine(AmiAdvancementDocument document) {
        List<String> parts = new ArrayList<>();
        if (!document.sourceId().isBlank()) {
            parts.add(ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(document.sourceId(), true)));
        }
        if (!document.tabTitle().isBlank()) {
            parts.add(document.tabTitle());
        }
        parts.add("Advancement");
        return String.join(" > ", parts);
    }

    private static String provenanceLine(AmiAdvancementDocument document) {
        List<String> parts = new ArrayList<>();
        if (!document.type().isBlank()) {
            parts.add(ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(document.type(), true)));
        }
        if (!document.description().isBlank()) {
            parts.add(document.description());
        }
        return parts.isEmpty() ? "Advancement match" : String.join(" - ", parts);
    }

    private static List<MatchEvidence> evidence(AmiAdvancementDocument document, List<String> tokens) {
        List<MatchEvidence> evidence = new ArrayList<>();
        addEvidence(evidence, tokens, document.title(), MatchEvidence.SourceType.ADVANCEMENT_TITLE, document.id(), document.title(), "");
        addEvidence(evidence, tokens, document.tabTitle(), MatchEvidence.SourceType.ADVANCEMENT_TAB, document.id(), document.tabTitle(), "");
        addEvidence(evidence, tokens, document.type(), MatchEvidence.SourceType.ADVANCEMENT_TYPE, document.id(), document.type(), "");
        if (document.iconItemId() != null) {
            ResourceLocation itemId = document.iconItemId();
            addEvidence(evidence, tokens, itemId.toString() + " " + itemId.getPath(), MatchEvidence.SourceType.ADVANCEMENT_ICON,
                    document.id(), formatId(itemId), "");
        }
        String snippet = matchedSnippet(document.description(), tokens);
        if (!snippet.isBlank()) {
            evidence.add(new MatchEvidence(MatchEvidence.SourceType.ADVANCEMENT_DESCRIPTION, document.id(), document.title(), snippet));
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
