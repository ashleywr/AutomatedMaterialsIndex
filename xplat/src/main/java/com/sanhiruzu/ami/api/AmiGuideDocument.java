package com.sanhiruzu.ami.api;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Source-agnostic searchable guide page/tutorial document.
 * <p>
 * AMI indexes these separately from normal {@code SearchNode}s so guide text
 * does not bloat item metadata. The owning mod or compat adapter remains
 * responsible for opening its actual guide UI through {@link #openAction()}.
 */
public record AmiGuideDocument(
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
    public AmiGuideDocument {
        if (id == null) {
            throw new IllegalArgumentException("Guide document id must not be null");
        }
        sourceType = clean(sourceType);
        modId = clean(modId);
        pageId = clean(pageId);
        title = cleanDisplayText(title);
        chapter = cleanDisplayText(chapter);
        referencedItems = referencedItems == null ? List.of() : List.copyOf(referencedItems);
        tags = cleanList(tags);
        summaryText = cleanSummaryText(summaryText);
        visibility = visibility == null ? () -> true : visibility;
    }

    public static Builder builder(Identifier id, String sourceType, String modId, String title) {
        return new Builder(id, sourceType, modId, title);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String cleanDisplayText(String value) {
        String cleaned = clean(value);
        return looksLikeTranslationKey(cleaned) ? readableTranslationFallback(cleaned) : cleaned;
    }

    private static String cleanSummaryText(String value) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            return "";
        }
        if (looksLikeTranslationKey(cleaned)) {
            return readableTranslationFallback(cleaned);
        }
        StringBuilder out = new StringBuilder(cleaned.length());
        for (String token : cleaned.split("\\s+")) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(cleanSummaryToken(token));
        }
        return out.toString();
    }

    private static String cleanSummaryToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        int start = 0;
        int end = token.length();
        while (start < end && !Character.isLetterOrDigit(token.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(token.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return token;
        }
        String core = token.substring(start, end);
        if (!looksLikeEmbeddedTranslationKey(core)) {
            return token;
        }
        return token.substring(0, start) + readableTranslationFallback(core) + token.substring(end);
    }

    private static boolean looksLikeTranslationKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String clean = value.trim();
        return clean.startsWith("item.")
                || clean.startsWith("block.")
                || clean.startsWith("book.")
                || clean.startsWith("entity.")
                || clean.startsWith("advancements.")
                || looksLikeModScopedTranslationKey(clean);
    }

    private static boolean looksLikeEmbeddedTranslationKey(String value) {
        return value != null
                && (value.startsWith("item.")
                || value.startsWith("block.")
                || value.startsWith("book.")
                || value.startsWith("entity.")
                || value.startsWith("advancements.")
                || looksLikeModScopedTranslationKeyWithAtLeastThreeSegments(value));
    }

    private static boolean looksLikeModScopedTranslationKey(String value) {
        return value.indexOf(':') < 0
                && value.contains(".")
                && value.matches("[a-z0-9_.-]+")
                && value.matches(".*[a-z].*");
    }

    private static boolean looksLikeModScopedTranslationKeyWithAtLeastThreeSegments(String value) {
        return looksLikeModScopedTranslationKey(value) && value.split("\\.").length >= 3;
    }

    private static String readableTranslationFallback(String raw) {
        String[] parts = raw.split("\\.");
        if (parts.length == 0) {
            return humanize(raw);
        }
        int index = parts.length - 1;
        while (index > 0 && isGenericTranslationLeaf(parts[index])) {
            index--;
        }
        return humanize(parts[Math.max(0, index)]);
    }

    private static boolean isGenericTranslationLeaf(String value) {
        return switch (value) {
            case "name", "title", "text", "description", "landing", "landing_text", "entries", "categories", "pages" ->
                    true;
            default -> false;
        };
    }

    private static String humanize(String raw) {
        String value = clean(raw);
        int slash = value.lastIndexOf('/');
        if (slash >= 0) {
            value = value.substring(slash + 1);
        }
        StringBuilder out = new StringBuilder();
        for (String part : value.split("[_\\-]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.isEmpty() ? "" : out.toString();
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

    public static final class Builder {
        private final Identifier id;
        private final String sourceType;
        private final String modId;
        private final String title;
        private final List<Identifier> referencedItems = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();
        private Identifier bookId;
        private Identifier iconItemId;
        private String pageId = "";
        private String chapter = "";
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

        public AmiGuideDocument build() {
            return new AmiGuideDocument(
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
