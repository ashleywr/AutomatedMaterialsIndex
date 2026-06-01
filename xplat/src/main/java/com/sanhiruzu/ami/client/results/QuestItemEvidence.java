package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestItemMatch;

import java.util.List;

/**
 * UI-ready quest evidence for a normal item search result.
 */
public record QuestItemEvidence(
        int totalCount,
        int requirementCount,
        int rewardCount,
        String badgeLabel,
        List<AmiQuestItemMatch> matches,
        List<String> tooltipLines
) {
    public QuestItemEvidence {
        totalCount = Math.max(0, totalCount);
        requirementCount = Math.max(0, requirementCount);
        rewardCount = Math.max(0, rewardCount);
        badgeLabel = badgeLabel == null ? "" : badgeLabel;
        matches = matches == null ? List.of() : List.copyOf(matches);
        tooltipLines = tooltipLines == null ? List.of() : List.copyOf(tooltipLines);
    }

    public boolean hasMatches() {
        return totalCount > 0;
    }

    public boolean hasRequirement() {
        return requirementCount > 0;
    }

    public boolean hasReward() {
        return rewardCount > 0;
    }
}
