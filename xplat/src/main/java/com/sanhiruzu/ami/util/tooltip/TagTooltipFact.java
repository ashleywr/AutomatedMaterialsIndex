package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TagTooltipFact implements AmiTooltipFact {
    private static final int PREVIEW_TAGS_SHOWN = 5;
    private static final int MAX_EXPANDED_TAGS_SHOWN = 32;

    @Override
    public List<Component> build(SearchNode entry) {
        if (!AmiConfig.showTooltipTags) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        appendTags(lines, sectionKey(entry), entry.meta(SearchNodeKeys.TAGS, ""));
        if (entry.type() == NodeType.ITEM) {
            appendTags(lines, "ami.tooltip.block_tags", entry.meta(SearchNodeKeys.BLOCK_TAGS, ""));
        }
        return lines;
    }

    private static String sectionKey(SearchNode entry) {
        return entry.type() == NodeType.ENTITY ? "ami.tooltip.entity_tags" : "ami.tooltip.item_tags";
    }

    private static void appendTags(List<Component> lines, String labelKey, String encodedTags) {
        List<String> tags = parseTags(encodedTags);
        if (tags.isEmpty()) {
            return;
        }

        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(Component.translatable("ami.tooltip.tag_heading", Component.translatable(labelKey), tags.size())
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));

        int limit = Screen.hasShiftDown() ? MAX_EXPANDED_TAGS_SHOWN : PREVIEW_TAGS_SHOWN;
        int shown = Math.min(tags.size(), limit);
        for (int i = 0; i < shown; i++) {
            lines.add(Component.translatable("ami.tooltip.tag_prefix", tags.get(i))
                    .withStyle(s -> s.withColor(AMITheme.POSITIVE)));
        }
        if (tags.size() > shown) {
            String key = Screen.hasShiftDown() ? "ami.tooltip.more_entries" : "ami.tooltip.more_entries_shift";
            lines.add(Component.translatable(key, tags.size() - shown)
                    .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
        }
    }

    static List<String> parseTags(String encodedTags) {
        if (encodedTags == null || encodedTags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(encodedTags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }
}
