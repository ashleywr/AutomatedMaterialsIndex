package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestItemMatch;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds compact item-row quest indicators from the rich quest index.
 */
public final class QuestItemEvidenceProjector {
    private static final int MAX_TOOLTIP_MATCHES = 4;

    private QuestItemEvidenceProjector() {
    }

    public static QuestItemEvidence project(SearchNode node) {
        if (!AmiConfig.searchIncludeQuests || node == null || node.type() != NodeType.ITEM) {
            return empty();
        }
        return project(AmiQuestsApi.getQuestMatchesForItem(node.id()));
    }

    public static QuestItemEvidence project(List<AmiQuestItemMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return empty();
        }

        List<AmiQuestItemMatch> cleanMatches = matches.stream()
                .filter(match -> match != null && match.quest() != null && match.task() != null)
                .toList();
        if (cleanMatches.isEmpty()) {
            return empty();
        }

        int requirements = 0;
        int rewards = 0;
        for (AmiQuestItemMatch match : cleanMatches) {
            if (match.task().role() == AmiQuestTaskDocument.Role.REWARD) {
                rewards++;
            } else if (match.task().role() == AmiQuestTaskDocument.Role.REQUIREMENT) {
                requirements++;
            }
        }

        int total = cleanMatches.size();
        return new QuestItemEvidence(
                total,
                requirements,
                rewards,
                badgeLabel(total),
                cleanMatches,
                tooltipLines(cleanMatches, requirements, rewards)
        );
    }

    private static QuestItemEvidence empty() {
        return new QuestItemEvidence(0, 0, 0, "", List.of(), List.of());
    }

    private static String badgeLabel(int total) {
        if (total > 99) {
            return "Q99+";
        }
        return "Q" + total;
    }

    private static List<String> tooltipLines(List<AmiQuestItemMatch> matches, int requirements, int rewards) {
        List<String> lines = new ArrayList<>();
        lines.add(summaryLine(requirements, rewards));

        int shown = 0;
        for (AmiQuestItemMatch match : matches) {
            if (shown++ >= MAX_TOOLTIP_MATCHES) {
                break;
            }
            lines.add(matchLine(match));
        }

        int remaining = matches.size() - shown;
        if (remaining > 0) {
            lines.add("+" + remaining + " more quest matches");
        }
        return List.copyOf(lines);
    }

    private static String summaryLine(int requirements, int rewards) {
        List<String> parts = new ArrayList<>();
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
        if (parts.isEmpty()) {
            parts.add("quest reference");
        }
        return "Quests: " + String.join(", ", parts);
    }

    private static String matchLine(AmiQuestItemMatch match) {
        AmiQuestDocument quest = match.quest();
        AmiQuestTaskDocument task = match.task();
        String role = roleLabel(task.role());
        String path = questPath(quest);
        String taskSuffix = taskSuffix(task, match.itemId());

        return role + ": " + path + taskSuffix;
    }

    private static String roleLabel(AmiQuestTaskDocument.Role role) {
        return switch (role == null ? AmiQuestTaskDocument.Role.REFERENCE : role) {
            case REQUIREMENT -> "Requirement";
            case REWARD -> "Reward";
            case REFERENCE -> "Quest";
        };
    }

    private static String questPath(AmiQuestDocument quest) {
        List<String> parts = new ArrayList<>();
        if (!quest.chapterTitle().isBlank()) {
            parts.add(quest.chapterTitle());
        }
        parts.add(quest.title().isBlank() ? quest.id() : quest.title());
        return String.join(" > ", parts);
    }

    private static String taskSuffix(AmiQuestTaskDocument task, ResourceLocation itemId) {
        List<String> parts = new ArrayList<>();
        if (task.requiredCount() > 1) {
            parts.add(task.requiredCount() + "x");
        }
        if (!task.title().isBlank()) {
            parts.add(task.title());
        } else if (itemId != null) {
            parts.add(formatId(itemId));
        }
        if (task.progress() > 0 || task.requiredCount() > 0) {
            parts.add(task.progress() + "/" + task.requiredCount());
        }
        return parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")";
    }

    private static String formatId(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        return ResultsGroupLabels.formatGroupLabel(ResultsGroupLabels.formatGroupKey(id.toString(), true));
    }
}
