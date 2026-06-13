package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Searchable client-visible advancement document.
 */
public record AmiAdvancementDocument(
        ResourceLocation id,
        String sourceId,
        String tabTitle,
        String title,
        String description,
        String type,
        ProgressStatus progressStatus,
        ResourceLocation iconItemId,
        Runnable openAction
) {
    public AmiAdvancementDocument {
        if (id == null) {
            throw new IllegalArgumentException("Advancement id must not be null");
        }
        sourceId = clean(sourceId);
        tabTitle = clean(tabTitle);
        title = clean(title);
        description = description == null ? "" : description.trim();
        type = clean(type);
        progressStatus = progressStatus == null ? ProgressStatus.UNKNOWN : progressStatus;
    }

    public static Builder builder(ResourceLocation id, String title) {
        return new Builder(id, title);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean canOpen() {
        return openAction != null;
    }

    public void open() {
        if (openAction != null) {
            openAction.run();
        }
    }

    public enum ProgressStatus {
        IN_PROGRESS("In progress"),
        NOT_STARTED("Not started"),
        COMPLETED("Completed"),
        UNKNOWN("Status unknown");

        private final String label;

        ProgressStatus(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String title;
        private String sourceId = "minecraft";
        private String tabTitle = "";
        private String description = "";
        private String type = "";
        private ProgressStatus progressStatus = ProgressStatus.UNKNOWN;
        private ResourceLocation iconItemId;
        private Runnable openAction;

        private Builder(ResourceLocation id, String title) {
            this.id = id;
            this.title = title;
            if (id != null) {
                this.sourceId = id.getNamespace();
            }
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder tabTitle(String tabTitle) {
            this.tabTitle = tabTitle;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder progressStatus(ProgressStatus progressStatus) {
            this.progressStatus = progressStatus;
            return this;
        }

        public Builder iconItemId(ResourceLocation iconItemId) {
            this.iconItemId = iconItemId;
            return this;
        }

        public Builder openAction(Runnable openAction) {
            this.openAction = openAction;
            return this;
        }

        public AmiAdvancementDocument build() {
            return new AmiAdvancementDocument(id, sourceId, tabTitle, title, description, type, progressStatus, iconItemId, openAction);
        }
    }
}
