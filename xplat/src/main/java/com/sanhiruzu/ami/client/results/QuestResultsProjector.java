package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts quest index hits into row data that the results UI can render
 * without knowing the source quest implementation.
 */
public final class QuestResultsProjector {
    private QuestResultsProjector() {
    }

    public static List<QuestResultRow> project(String query, AmiQuestSearchIndex questIndex) {
        if (questIndex == null || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<QuestResultRow> rows = new ArrayList<>();
        for (AmiQuestDocument document : questIndex.search(query)) {
            rows.add(new QuestResultRow(
                    document,
                    document.title(),
                    sourceLine(document),
                    provenanceLine(document),
                    countRole(document, AmiQuestTaskDocument.Role.REQUIREMENT),
                    countRole(document, AmiQuestTaskDocument.Role.REWARD),
                    evidence(document, tokens)
            ));
        }
        return rows;
    }

    private static String sourceLine(AmiQuestDocument document) {
        List<String> parts = new ArrayList<>();
        if (!document.sourceId().isBlank()) {
            parts.add(sourceLabel(document.sourceId()));
        }
        if (!document.chapterTitle().isBlank()) {
            parts.add(document.chapterTitle());
        }
        parts.add("Quest");
        return String.join(" > ", parts);
    }

    private static String provenanceLine(AmiQuestDocument document) {
        int requirements = countRole(document, AmiQuestTaskDocument.Role.REQUIREMENT);
        int rewards = countRole(document, AmiQuestTaskDocument.Role.REWARD);
        List<String> parts = new ArrayList<>();
        parts.add(statusLabel(document.status()));
        if (requirements == 1) {
            parts.add("1 requirement");
        } else if (requirements > 1) {
            parts.add(requirements + " requirements");
        }
        if (rewards == 1) {
            parts.add("1 reward");
        } else if (rewards > 1) {
            parts.add(rewards + " rewards");
        }
        return String.join(" - ", parts);
    }

    private static List<MatchEvidence> evidence(AmiQuestDocument document, List<String> tokens) {
        List<MatchEvidence> evidence = new ArrayList<>();
        ResourceLocation sourceId = sourceId(document);
        addEvidence(evidence, tokens, document.title(), MatchEvidence.SourceType.QUEST_TITLE, sourceId, document.title(), "");
        addEvidence(evidence, tokens, document.chapterTitle(), MatchEvidence.SourceType.QUEST_CHAPTER, sourceId, document.chapterTitle(), "");
        addEvidence(evidence, tokens, document.status().name(), MatchEvidence.SourceType.QUEST_STATUS, sourceId, statusLabel(document.status()), "");
        for (AmiQuestTaskDocument task : document.tasks()) {
            addEvidence(evidence, tokens, task.title(), MatchEvidence.SourceType.QUEST_TASK, sourceId, task.title(), taskLine(task));
            for (ResourceLocation itemId : task.itemIds()) {
                String itemLabel = formatId(itemId);
                addEvidence(evidence, tokens, itemId.toString() + " " + itemId.getPath(), MatchEvidence.SourceType.QUEST_ITEM,
                        sourceId, itemLabel, taskLine(task));
            }
        }
        String snippet = matchedSnippet(document.description(), tokens);
        if (!snippet.isBlank()) {
            evidence.add(new MatchEvidence(MatchEvidence.SourceType.QUEST_DESCRIPTION, sourceId, document.title(), snippet));
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

    private static int countRole(AmiQuestDocument document, AmiQuestTaskDocument.Role role) {
        int count = 0;
        for (AmiQuestTaskDocument task : document.tasks()) {
            if (task.role() == role) {
                count++;
            }
        }
        return count;
    }

    private static String taskLine(AmiQuestTaskDocument task) {
        List<String> parts = new ArrayList<>();
        parts.add(task.role() == AmiQuestTaskDocument.Role.REWARD ? "Reward" : "Requirement");
        if (task.requiredCount() > 1) {
            parts.add(task.requiredCount() + "x");
        }
        if (!task.itemIds().isEmpty()) {
            parts.add(formatId(task.itemIds().get(0)));
        } else if (!task.title().isBlank()) {
            parts.add(task.title());
        }
        if (task.progress() > 0 || task.requiredCount() > 0) {
            parts.add(task.progress() + "/" + task.requiredCount());
        }
        return String.join(" - ", parts);
    }

    private static String statusLabel(AmiQuestDocument.Status status) {
        return switch (status == null ? AmiQuestDocument.Status.UNKNOWN : status) {
            case COMPLETED -> "Completed";
            case STARTED -> "Started";
            case AVAILABLE -> "Available";
            case LOCKED -> "Locked";
            case UNKNOWN -> "Quest match";
        };
    }

    private static String sourceLabel(String sourceId) {
        if ("ftbquests".equals(sourceId)) {
            return "FTB Quests";
        }
        return ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(sourceId, true));
    }

    private static ResourceLocation sourceId(AmiQuestDocument document) {
        String id = document.id();
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? ResourceLocation.fromNamespaceAndPath("ami", "quest/" + Math.abs(id.hashCode())) : parsed;
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
