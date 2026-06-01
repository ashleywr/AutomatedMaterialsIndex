package com.sanhiruzu.ami.client.results;

import net.minecraft.resources.ResourceLocation;

/**
 * UI-facing explanation for why a non-item source contributed to a result.
 */
public record MatchEvidence(
        SourceType sourceType,
        ResourceLocation sourceId,
        String label,
        String snippet
) {
    public MatchEvidence {
        label = label == null ? "" : label;
        snippet = snippet == null ? "" : snippet;
    }

    public enum SourceType {
        GUIDE_TITLE,
        GUIDE_CHAPTER,
        GUIDE_TAG,
        GUIDE_REFERENCE,
        GUIDE_SUMMARY,
        QUEST_TITLE,
        QUEST_CHAPTER,
        QUEST_STATUS,
        QUEST_TASK,
        QUEST_ITEM,
        QUEST_DESCRIPTION
    }
}
