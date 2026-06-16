package com.sanhiruzu.searchableguides.api;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * UI-agnostic searchable guide page contributed by a mod-owned guide system.
 * <p>
 * This contract intentionally does not mention AMI, EMI, JEI, or any viewer UI.
 * Viewers can adapt these documents into their own search/result surfaces.
 */
public record SearchableGuideDocument(
        Identifier id,
        String sourceType,
        String modId,
        Identifier bookId,
        Identifier iconItemId,
        String pageId,
        String title,
        String chapter,
        List<Identifier> referencedItems,
        List<String> tags,
        String summaryText,
        BooleanSupplier visibility,
        Runnable openAction
) {
    public SearchableGuideDocument {
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
        visibility = visibility == null ? () -> true : visibility;
    }

    public boolean isVisible() {
        try {
            return visibility.getAsBoolean();
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public boolean canOpen() {
        return openAction != null;
    }

    public void open() {
        if (openAction != null) {
            openAction.run();
        }
    }

    public static Builder builder(Identifier id, String sourceType, String modId, String title) {
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
        private final Identifier id;
        private final String sourceType;
        private final String modId;
        private final String title;
        private Identifier bookId;
        private Identifier iconItemId;
        private String pageId = "";
        private String chapter = "";
        private final List<Identifier> referencedItems = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();
        private String summaryText = "";
        private BooleanSupplier visibility;
        private Runnable openAction;

        private Builder(Identifier id, String sourceType, String modId, String title) {
            this.id = id;
            this.sourceType = sourceType;
            this.modId = modId;
            this.title = title;
        }

        public Builder bookId(Identifier bookId) {
            this.bookId = bookId;
            return this;
        }

        public Builder iconItemId(Identifier iconItemId) {
            this.iconItemId = iconItemId;
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

        public Builder referencedItem(Identifier itemId) {
            if (itemId != null && !referencedItems.contains(itemId)) {
                referencedItems.add(itemId);
            }
            return this;
        }

        public Builder referencedItems(List<Identifier> itemIds) {
            if (itemIds != null) {
                for (Identifier itemId : itemIds) {
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

        public Builder visibility(BooleanSupplier visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder openAction(Runnable openAction) {
            this.openAction = openAction;
            return this;
        }

        public SearchableGuideDocument build() {
            return new SearchableGuideDocument(
                    id,
                    sourceType,
                    modId,
                    bookId,
                    iconItemId,
                    pageId,
                    title,
                    chapter,
                    referencedItems,
                    tags,
                    summaryText,
                    visibility,
                    openAction
            );
        }
    }
}
