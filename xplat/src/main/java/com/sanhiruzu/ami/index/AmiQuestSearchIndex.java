package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AmiQuestSearchIndex {
    private final List<AmiQuestDocument> documents = new ArrayList<>();
    private final Map<AmiQuestDocument, String> searchableText = new LinkedHashMap<>();
    private final Map<ResourceLocation, List<AmiQuestItemMatch>> byItem = new LinkedHashMap<>();

    public AmiQuestSearchIndex(Collection<AmiQuestDocument> documents) {
        if (documents == null) {
            return;
        }
        for (AmiQuestDocument document : documents) {
            if (document == null) {
                continue;
            }
            this.documents.add(document);
            searchableText.put(document, buildSearchableText(document));
            indexItems(document);
        }
        this.documents.sort(Comparator.comparing(AmiQuestDocument::id));
        for (List<AmiQuestItemMatch> matches : byItem.values()) {
            matches.sort(Comparator.comparing((AmiQuestItemMatch match) -> match.quest().id())
                    .thenComparing(match -> match.task().id()));
        }
    }

    public List<AmiQuestDocument> allDocuments() {
        return List.copyOf(documents);
    }

    public List<AmiQuestDocument> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<String> tokens = tokens(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<ScoredQuest> scored = new ArrayList<>();
        for (AmiQuestDocument document : documents) {
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
                scored.add(new ScoredQuest(document, score));
            }
        }

        scored.sort(Comparator.comparingInt(ScoredQuest::score).reversed()
                .thenComparing(scoredQuest -> scoredQuest.document().id()));
        List<AmiQuestDocument> results = new ArrayList<>();
        for (ScoredQuest scoredQuest : scored) {
            results.add(scoredQuest.document());
        }
        return results;
    }

    public List<AmiQuestItemMatch> findItem(ResourceLocation itemId) {
        if (itemId == null) {
            return List.of();
        }
        return List.copyOf(byItem.getOrDefault(itemId, List.of()));
    }

    private void indexItems(AmiQuestDocument document) {
        for (AmiQuestTaskDocument task : document.tasks()) {
            for (ResourceLocation itemId : task.itemIds()) {
                byItem.computeIfAbsent(itemId, ignored -> new ArrayList<>())
                        .add(new AmiQuestItemMatch(document, task, itemId));
            }
        }
    }

    private static String buildSearchableText(AmiQuestDocument document) {
        LinkedHashSet<String> fields = new LinkedHashSet<>();
        fields.add(document.id());
        fields.add(document.sourceType());
        fields.add(document.sourceId());
        fields.add(document.chapterId());
        fields.add(document.chapterTitle());
        fields.add(document.title());
        fields.add(document.description());
        fields.add(document.status().name());
        for (AmiQuestTaskDocument task : document.tasks()) {
            fields.add(task.id());
            fields.add(task.role().name());
            fields.add(task.taskType());
            fields.add(task.title());
            for (ResourceLocation itemId : task.itemIds()) {
                fields.add(itemId.toString());
                fields.add(itemId.getNamespace());
                fields.add(itemId.getPath());
            }
            fields.addAll(task.tags());
        }
        return normalize(String.join(" ", fields));
    }

    private static int scoreToken(AmiQuestDocument document, String token) {
        int score = 0;
        if (contains(document.title(), token)) score += 50;
        if (contains(document.chapterTitle(), token)) score += 35;
        if (contains(document.sourceId(), token)) score += 25;
        if (contains(document.id(), token)) score += 20;
        for (AmiQuestTaskDocument task : document.tasks()) {
            if (contains(task.title(), token)) score += 30;
            for (ResourceLocation itemId : task.itemIds()) {
                if (contains(itemId.toString(), token) || contains(itemId.getPath(), token)) {
                    score += 35;
                    break;
                }
            }
        }
        if (contains(document.description(), token)) score += 5;
        return score;
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

    private record ScoredQuest(AmiQuestDocument document, int score) {
    }
}
