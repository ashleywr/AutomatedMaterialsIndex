package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Source-agnostic searchable guide page/tutorial document.
 * <p>
 * AMI indexes these separately from normal {@code SearchNode}s so guide text
 * does not bloat item metadata. The owning mod or compat adapter remains
 * responsible for opening its actual guide UI through {@link #openAction()}.
 */
public record AmiGuideDocument(
        ResourceLocation id,
        String sourceType,
        String modId,
        ResourceLocation bookId,
        String pageId,
        String title,
        String chapter,
        List<ResourceLocation> referencedItems,
        List<String> tags,
        String summaryText,
        Runnable openAction
) {
    public AmiGuideDocument {
        if (id == null) {
            throw new IllegalArgumentException("Guide document id must not be null");
        }
        sourceType = clean(sourceType);
        modId = clean(modId);
        pageId = clean(pageId);
        title = clean(title);
        chapter = clean(chapter);
        referencedItems = referencedItems == null ? List.of() : List.copyOf(referencedItems);
        tags = cleanList(tags);
        summaryText = summaryText == null ? "" : summaryText;
    }

    public boolean canOpen() {
        return openAction != null;
    }

    public void open() {
        if (openAction != null) {
            openAction.run();
        }
    }

    public static Builder builder(ResourceLocation id, String sourceType, String modId, String title) {
        return new Builder(id, sourceType, modId, title);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
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

    public static final class Builder {
        private final ResourceLocation id;
        private final String sourceType;
        private final String modId;
        private final String title;
        private ResourceLocation bookId;
        private String pageId = "";
        private String chapter = "";
        private final List<ResourceLocation> referencedItems = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();
        private String summaryText = "";
        private Runnable openAction;

        private Builder(ResourceLocation id, String sourceType, String modId, String title) {
            this.id = id;
            this.sourceType = sourceType;
            this.modId = modId;
            this.title = title;
        }

        public Builder bookId(ResourceLocation bookId) {
            this.bookId = bookId;
            return this;
        }

        public Builder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public Builder chapter(String chapter) {
            this.chapter = chapter;
            return this;
        }

        public Builder referencedItem(ResourceLocation itemId) {
            if (itemId != null && !referencedItems.contains(itemId)) {
                referencedItems.add(itemId);
            }
            return this;
        }

        public Builder referencedItems(List<ResourceLocation> itemIds) {
            if (itemIds != null) {
                for (ResourceLocation itemId : itemIds) {
                    referencedItem(itemId);
                }
            }
            return this;
        }

        public Builder tag(String tag) {
            String clean = clean(tag);
            if (!clean.isEmpty() && !tags.contains(clean)) {
                tags.add(clean);
            }
            return this;
        }

        public Builder tags(List<String> tags) {
            if (tags != null) {
                for (String tag : tags) {
                    tag(tag);
                }
            }
            return this;
        }

        public Builder summaryText(String summaryText) {
            this.summaryText = summaryText == null ? "" : summaryText;
            return this;
        }

        public Builder openAction(Runnable openAction) {
            this.openAction = openAction;
            return this;
        }

        public AmiGuideDocument build() {
            return new AmiGuideDocument(
                    id,
                    sourceType,
                    modId,
                    bookId,
                    pageId,
                    title,
                    chapter,
                    referencedItems,
                    tags,
                    summaryText,
                    openAction
            );
        }
    }
}
