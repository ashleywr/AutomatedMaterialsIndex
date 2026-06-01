package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Source-agnostic item/task data for a quest.
 * <p>
 * This is intentionally small: it stores the stable ids and bounded item
 * references AMI needs for search, context menus, and pack-author diagnostics.
 */
public record AmiQuestTaskDocument(
        String id,
        String questId,
        Role role,
        String taskType,
        String title,
        List<ResourceLocation> itemIds,
        long requiredCount,
        long progress,
        boolean consumesItems,
        boolean craftingOnly,
        boolean highCardinality,
        List<String> tags
) {
    public AmiQuestTaskDocument {
        id = clean(id);
        questId = clean(questId);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Quest task id must not be blank");
        }
        if (questId.isEmpty()) {
            throw new IllegalArgumentException("Quest task questId must not be blank");
        }
        role = role == null ? Role.REQUIREMENT : role;
        taskType = clean(taskType);
        title = clean(title);
        itemIds = cleanItems(itemIds);
        requiredCount = Math.max(0L, requiredCount);
        progress = Math.max(0L, progress);
        tags = cleanList(tags);
    }

    public static Builder builder(String id, String questId, Role role) {
        return new Builder(id, questId, role);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<ResourceLocation> cleanItems(List<ResourceLocation> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> cleaned = new ArrayList<>();
        for (ResourceLocation value : values) {
            if (value != null && !cleaned.contains(value)) {
                cleaned.add(value);
            }
        }
        return List.copyOf(cleaned);
    }

    private static List<String> cleanList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            String clean = clean(value);
            if (!clean.isEmpty() && !cleaned.contains(clean)) {
                cleaned.add(clean);
            }
        }
        return List.copyOf(cleaned);
    }

    public enum Role {
        REQUIREMENT,
        REWARD,
        REFERENCE
    }

    public static final class Builder {
        private final String id;
        private final String questId;
        private final Role role;
        private String taskType = "";
        private String title = "";
        private final List<ResourceLocation> itemIds = new ArrayList<>();
        private long requiredCount;
        private long progress;
        private boolean consumesItems;
        private boolean craftingOnly;
        private boolean highCardinality;
        private final List<String> tags = new ArrayList<>();

        private Builder(String id, String questId, Role role) {
            this.id = id;
            this.questId = questId;
            this.role = role;
        }

        public Builder taskType(String taskType) {
            this.taskType = taskType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder itemId(ResourceLocation itemId) {
            if (itemId != null && !itemIds.contains(itemId)) {
                itemIds.add(itemId);
            }
            return this;
        }

        public Builder itemIds(List<ResourceLocation> itemIds) {
            if (itemIds != null) {
                for (ResourceLocation itemId : itemIds) {
                    itemId(itemId);
                }
            }
            return this;
        }

        public Builder requiredCount(long requiredCount) {
            this.requiredCount = requiredCount;
            return this;
        }

        public Builder progress(long progress) {
            this.progress = progress;
            return this;
        }

        public Builder consumesItems(boolean consumesItems) {
            this.consumesItems = consumesItems;
            return this;
        }

        public Builder craftingOnly(boolean craftingOnly) {
            this.craftingOnly = craftingOnly;
            return this;
        }

        public Builder highCardinality(boolean highCardinality) {
            this.highCardinality = highCardinality;
            return this;
        }

        public Builder tag(String tag) {
            String clean = clean(tag);
            if (!clean.isEmpty() && !tags.contains(clean)) {
                tags.add(clean);
            }
            return this;
        }

        public AmiQuestTaskDocument build() {
            return new AmiQuestTaskDocument(
                    id,
                    questId,
                    role,
                    taskType,
                    title,
                    itemIds,
                    requiredCount,
                    progress,
                    consumesItems,
                    craftingOnly,
                    highCardinality,
                    tags
            );
        }
    }
}
