package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.api.AmiQuestEntry;
import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestGroup;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.client.results.TreeNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class QuestSidebarProjector {
    public static final String QUEST_FALLBACK = "questFallback";

    private QuestSidebarProjector() {
    }

    public static List<TreeNode> project(List<AmiQuestGroup> groups,
                                         Function<ResourceLocation, Optional<SearchNode>> nodeResolver) {
        return project(groups, List.of(), nodeResolver);
    }

    public static List<TreeNode> project(List<AmiQuestGroup> groups,
                                         List<AmiQuestDocument> documents,
                                         Function<ResourceLocation, Optional<SearchNode>> nodeResolver) {
        boolean hasGroups = groups != null && !groups.isEmpty();
        boolean hasDocuments = documents != null && !documents.isEmpty();
        if (!hasGroups && !hasDocuments) {
            return List.of();
        }

        Function<ResourceLocation, Optional<SearchNode>> resolver =
                nodeResolver == null ? ignored -> Optional.empty() : nodeResolver;

        List<TreeNode> roots = new ArrayList<>();
        roots.addAll(projectGroups(groups, resolver));
        roots.addAll(projectDocuments(documents, resolver));
        return List.copyOf(roots);
    }

    private static List<TreeNode> projectGroups(List<AmiQuestGroup> groups,
                                                Function<ResourceLocation, Optional<SearchNode>> resolver) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }

        List<TreeNode> roots = new ArrayList<>();
        for (AmiQuestGroup group : groups) {
            if (group == null) {
                continue;
            }
            Map<ResourceLocation, Integer> counts = aggregateCounts(group.entries());
            TreeNode groupNode = new TreeNode(group.id(), group.label());
            groupNode.setExpanded(true);
            for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
                SearchNode node = resolver.apply(entry.getKey())
                        .orElseGet(() -> fallbackNode(entry.getKey()));
                groupNode.addChild(new TreeNode(labelFor(node, entry.getValue()), node));
            }
            if (!groupNode.getChildren().isEmpty()) {
                roots.add(groupNode);
            }
        }
        return List.copyOf(roots);
    }

    private static List<TreeNode> projectDocuments(List<AmiQuestDocument> documents,
                                                   Function<ResourceLocation, Optional<SearchNode>> resolver) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<String, TreeNode> chapters = new LinkedHashMap<>();
        for (AmiQuestDocument document : documents) {
            if (document == null) {
                continue;
            }
            TreeNode questNode = questNode(document, resolver);
            if (questNode.getChildren().isEmpty()) {
                continue;
            }

            String chapterKey = document.chapterId().isBlank()
                    ? document.sourceId() + ":quests"
                    : document.chapterId();
            TreeNode chapterNode = chapters.computeIfAbsent(chapterKey, ignored -> {
                TreeNode node = new TreeNode(chapterKey, Component.literal(chapterLabel(document)));
                node.setExpanded(true);
                return node;
            });
            chapterNode.addChild(questNode);
        }

        return chapters.values().stream()
                .filter(chapter -> !chapter.getChildren().isEmpty())
                .toList();
    }

    private static TreeNode questNode(AmiQuestDocument document,
                                      Function<ResourceLocation, Optional<SearchNode>> resolver) {
        TreeNode questNode = new TreeNode(document.id(), Component.literal(questLabel(document)));
        questNode.setExpanded(true);

        Map<ResourceLocation, Integer> counts = aggregateDocumentRequirements(document);
        for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
            SearchNode node = resolver.apply(entry.getKey())
                    .orElseGet(() -> fallbackNode(entry.getKey()));
            questNode.addChild(new TreeNode(labelFor(node, entry.getValue()), node));
        }
        return questNode;
    }

    private static Map<ResourceLocation, Integer> aggregateDocumentRequirements(AmiQuestDocument document) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (AmiQuestTaskDocument task : document.tasks()) {
            if (task.role() != AmiQuestTaskDocument.Role.REQUIREMENT) {
                continue;
            }
            int requiredCount = safeCount(task.requiredCount());
            for (ResourceLocation itemId : task.itemIds()) {
                counts.merge(itemId, requiredCount, Integer::sum);
            }
        }
        return counts;
    }

    private static int safeCount(long requiredCount) {
        if (requiredCount <= 0) {
            return 1;
        }
        return (int) Math.min(Integer.MAX_VALUE, requiredCount);
    }

    private static String chapterLabel(AmiQuestDocument document) {
        if (!document.chapterTitle().isBlank()) {
            return document.chapterTitle();
        }
        if (!document.sourceId().isBlank()) {
            return document.sourceId();
        }
        return "Quests";
    }

    private static String questLabel(AmiQuestDocument document) {
        return document.title().isBlank() ? document.id() : document.title();
    }

    private static Map<ResourceLocation, Integer> aggregateCounts(List<AmiQuestEntry> entries) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        if (entries == null) {
            return counts;
        }
        for (AmiQuestEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            counts.merge(entry.itemId(), entry.requiredCount(), Integer::sum);
        }
        return counts;
    }

    private static Component labelFor(SearchNode node, int requiredCount) {
        if (requiredCount > 1) {
            return Component.translatable("ami.tooltip.quest_item_count", node.displayName(), requiredCount);
        }
        return Component.literal(node.displayName());
    }

    private static SearchNode fallbackNode(ResourceLocation itemId) {
        Map<String, String> metadata = Map.of(
                SearchNodeKeys.MOD_ID, itemId.getNamespace(),
                QUEST_FALLBACK, "true"
        );
        return new SearchNode(itemId, NodeType.ITEM, itemId.toString(), 0, 0, metadata);
    }
}
